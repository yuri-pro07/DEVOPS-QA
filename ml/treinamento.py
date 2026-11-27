import json
import os
import uuid
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
from joblib import dump
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

PASTA_DOS_MODELOS = Path(os.getenv("PASTA_DOS_MODELOS", "/models"))
ARQUIVO_DO_MODELO = PASTA_DOS_MODELOS / "competencia.joblib"
ARQUIVO_DE_METADADOS = PASTA_DOS_MODELOS / "competencia.json"
NOME_DO_MODELO = "regressao-logistica"
CARACTERISTICAS = [
    "mesesExperiencia",
    "cursosConcluidos",
    "mediaAvaliacoes",
    "horasSemanais",
    "conheceRest",
    "conheceBancoDeDados",
]
NIVEIS = ["INICIANTE", "INTERMEDIARIO", "AVANCADO"]
REGRA_DE_ROTULAGEM = "pontuacao-v2"
AMOSTRAS_PADRAO = 3000


def gerar_dados(semente, quantidade=AMOSTRAS_PADRAO):
    rng = np.random.default_rng(semente)
    x = np.column_stack([
        rng.integers(0, 120, quantidade),
        rng.integers(0, 30, quantidade),
        rng.uniform(0, 10, quantidade),
        rng.integers(0, 40, quantidade),
        rng.integers(0, 2, quantidade),
        rng.integers(0, 2, quantidade),
    ]).astype(float)
    pontuacao = (x[:, 0] * 0.25 + x[:, 1] * 1.2 + x[:, 2] * 2.0 + x[:, 3] * 0.3
                 + x[:, 4] * 4 + x[:, 5] * 4 + rng.normal(0, 2, quantidade))
    y = np.where(pontuacao < 40, NIVEIS[0], np.where(pontuacao < 58, NIVEIS[1], NIVEIS[2]))
    return x, y


def precisa_treinar():
    if not ARQUIVO_DO_MODELO.exists() or not ARQUIVO_DE_METADADOS.exists():
        return True
    metadados = json.loads(ARQUIVO_DE_METADADOS.read_text(encoding="utf-8"))
    return metadados.get("regra") != REGRA_DE_ROTULAGEM


def gravar_atomicamente(destino, gravar):
    temporario = destino.with_name(destino.name + "." + uuid.uuid4().hex + ".tmp")
    gravar(temporario)
    temporario.replace(destino)


def treinar(semente=42, amostras=AMOSTRAS_PADRAO):
    x, y = gerar_dados(semente, amostras)
    x_treino, x_teste, y_treino, y_teste = train_test_split(
        x, y, test_size=0.2, stratify=y, random_state=semente)
    modelo = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, random_state=semente))
    modelo.fit(x_treino, y_treino)
    previsto = modelo.predict(x_teste)
    agora = datetime.now(timezone.utc)
    metadados = {
        "modelo": NOME_DO_MODELO,
        "versao": agora.strftime("%Y%m%d%H%M%S%f")[:17],
        "semente": semente,
        "regra": REGRA_DE_ROTULAGEM,
        "caracteristicas": CARACTERISTICAS,
        "amostras_de_treino": int(len(x_treino)),
        "amostras_de_teste": int(len(x_teste)),
        "acuracia": round(float(accuracy_score(y_teste, previsto)), 4),
        "f1_macro": round(float(f1_score(y_teste, previsto, average="macro")), 4),
        "dados": "sinteticos",
        "treinado_em": agora.isoformat(),
    }
    PASTA_DOS_MODELOS.mkdir(parents=True, exist_ok=True)
    texto = json.dumps(metadados, ensure_ascii=False, indent=2)
    gravar_atomicamente(ARQUIVO_DE_METADADOS, lambda caminho: caminho.write_text(texto, encoding="utf-8"))
    gravar_atomicamente(ARQUIVO_DO_MODELO, lambda caminho: dump(modelo, caminho))
    return metadados
