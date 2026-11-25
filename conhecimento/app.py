import os
import re
from pathlib import Path

from fastapi import FastAPI, Query
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

PASTA_DOS_DOCUMENTOS = Path(os.environ.get("PASTA_DOS_DOCUMENTOS", Path(__file__).parent / "documentos"))
PALAVRAS_VAZIAS = ["a", "ao", "aos", "as", "com", "como", "da", "das", "de", "do", "dos", "e", "em", "entre",
                   "esta", "eu", "mais", "mas", "na", "nas", "no", "nos", "o", "os", "ou", "para", "pela",
                   "pelo", "por", "que", "quer", "quero", "se", "sem", "ser", "sua", "seu", "um", "uma"]


def carregar_trechos(pasta):
    trechos = []
    for arquivo in sorted(pasta.glob("*.md")):
        cabecalho, *secoes = re.split(r"^## ", arquivo.read_text(encoding="utf-8"), flags=re.MULTILINE)
        documento = cabecalho.strip().lstrip("#").strip()
        for numero, secao in enumerate(secoes, start=1):
            titulo, _, corpo = secao.partition("\n")
            trechos.append({"id": f"{arquivo.stem}#{numero}", "documento": documento, "fonte": arquivo.name,
                            "secao": titulo.strip(), "texto": " ".join(corpo.split())})
    return trechos


class Indice:
    def __init__(self, trechos):
        self.trechos = trechos
        self.vetorizador = TfidfVectorizer(strip_accents="unicode", ngram_range=(1, 2), sublinear_tf=True,
                                           stop_words=PALAVRAS_VAZIAS)
        self.matriz = self.vetorizador.fit_transform(
            [t["documento"] + " " + t["secao"] + " " + t["texto"] for t in trechos])

    def buscar(self, consulta, quantidade):
        pontuacoes = cosine_similarity(self.vetorizador.transform([consulta]), self.matriz)[0]
        ordem = pontuacoes.argsort()[::-1][:quantidade]
        return [dict(self.trechos[i], relevancia=round(float(pontuacoes[i]), 4)) for i in ordem if pontuacoes[i] > 0]


indice = Indice(carregar_trechos(PASTA_DOS_DOCUMENTOS))
app = FastAPI(title="Servico de Conhecimento - Documentos Institucionais", version="1.0")


@app.get("/health")
def saude():
    return {"status": "ok", "documentos": len({t["fonte"] for t in indice.trechos}), "trechos": len(indice.trechos)}


@app.get("/documentos")
def documentos():
    agrupados = {}
    for trecho in indice.trechos:
        agrupados.setdefault(trecho["fonte"], {"fonte": trecho["fonte"], "documento": trecho["documento"],
                                               "secoes": []})["secoes"].append(trecho["secao"])
    return list(agrupados.values())


@app.get("/buscar")
def buscar(q: str = Query(min_length=2, max_length=500), k: int = Query(default=4, ge=1, le=10)):
    return {"consulta": q, "trechos": indice.buscar(q, k)}
