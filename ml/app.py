import json
from contextlib import asynccontextmanager
from threading import Lock

from fastapi import FastAPI, HTTPException
from joblib import load
from pydantic import BaseModel, Field

from treinamento import ARQUIVO_DE_METADADOS, ARQUIVO_DO_MODELO, NOME_DO_MODELO, precisa_treinar, treinar

trava = Lock()
cache = {"modelo": None, "versao": None, "carimbo": None}


class Perfil(BaseModel):
    mesesExperiencia: int = Field(ge=0, le=600)
    cursosConcluidos: int = Field(ge=0, le=100)
    mediaAvaliacoes: float = Field(ge=0, le=10)
    horasSemanais: int = Field(ge=0, le=80)
    conheceRest: bool
    conheceBancoDeDados: bool


def modelo_atual():
    with trava:
        if not ARQUIVO_DO_MODELO.exists():
            treinar()
        carimbo = ARQUIVO_DO_MODELO.stat().st_mtime_ns
        if cache["modelo"] is None or carimbo != cache["carimbo"]:
            cache["modelo"] = load(ARQUIVO_DO_MODELO)
            cache["versao"] = metadados()["versao"]
            cache["carimbo"] = carimbo
        return cache["modelo"], cache["versao"]


def metadados():
    if not ARQUIVO_DE_METADADOS.exists():
        raise HTTPException(status_code=404, detail="Modelo ainda nao treinado")
    return json.loads(ARQUIVO_DE_METADADOS.read_text(encoding="utf-8"))


@asynccontextmanager
async def ciclo_de_vida(_):
    if precisa_treinar():
        with trava:
            treinar()
    modelo_atual()
    yield


app = FastAPI(title="Servico de ML - Nivel de Competencia", version="1.0", lifespan=ciclo_de_vida)


@app.get("/health")
def saude():
    return {"status": "ok", "modelo_pronto": ARQUIVO_DO_MODELO.exists()}


@app.get("/modelo")
def modelo():
    return metadados()


@app.post("/predict")
def classificar(perfil: Perfil):
    classificador, versao = modelo_atual()
    linha = [[perfil.mesesExperiencia, perfil.cursosConcluidos, perfil.mediaAvaliacoes,
              perfil.horasSemanais, int(perfil.conheceRest), int(perfil.conheceBancoDeDados)]]
    probabilidades = classificador.predict_proba(linha)[0]
    return {
        "nivel": str(classificador.classes_[probabilidades.argmax()]),
        "probabilidades": {str(n): round(float(p), 4) for n, p in zip(classificador.classes_, probabilidades)},
        "modelo": NOME_DO_MODELO + " v" + versao,
    }
