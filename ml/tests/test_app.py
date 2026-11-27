from fastapi.testclient import TestClient

from app import app

JUNIOR = {"mesesExperiencia": 6, "cursosConcluidos": 1, "mediaAvaliacoes": 6.0,
          "horasSemanais": 5, "conheceRest": False, "conheceBancoDeDados": False}
PLENO = {"mesesExperiencia": 30, "cursosConcluidos": 10, "mediaAvaliacoes": 7.5,
         "horasSemanais": 12, "conheceRest": True, "conheceBancoDeDados": False}
SENIOR = {"mesesExperiencia": 96, "cursosConcluidos": 25, "mediaAvaliacoes": 9.5,
          "horasSemanais": 30, "conheceRest": True, "conheceBancoDeDados": True}


def test_servico_sobe_com_modelo_treinado():
    with TestClient(app) as cliente:
        resposta = cliente.get("/health")
    assert resposta.status_code == 200
    assert resposta.json()["modelo_pronto"] is True


def test_classifica_perfil_iniciante():
    with TestClient(app) as cliente:
        resposta = cliente.post("/predict", json=JUNIOR)
    assert resposta.status_code == 200
    assert resposta.json()["nivel"] == "INICIANTE"


def test_classifica_perfil_intermediario():
    with TestClient(app) as cliente:
        resposta = cliente.post("/predict", json=PLENO)
    assert resposta.json()["nivel"] == "INTERMEDIARIO"


def test_classifica_perfil_avancado():
    with TestClient(app) as cliente:
        resposta = cliente.post("/predict", json=SENIOR)
    assert resposta.json()["nivel"] == "AVANCADO"


def test_probabilidades_cobrem_os_tres_niveis_e_somam_um():
    with TestClient(app) as cliente:
        corpo = cliente.post("/predict", json=JUNIOR).json()
    assert set(corpo["probabilidades"]) == {"INICIANTE", "INTERMEDIARIO", "AVANCADO"}
    assert abs(sum(corpo["probabilidades"].values()) - 1) < 0.01
    assert corpo["modelo"].startswith("regressao-logistica v")


def test_recusa_media_acima_de_dez():
    with TestClient(app) as cliente:
        resposta = cliente.post("/predict", json=dict(JUNIOR, mediaAvaliacoes=11))
    assert resposta.status_code == 422


def test_metadados_mostram_a_qualidade_do_modelo():
    with TestClient(app) as cliente:
        corpo = cliente.get("/modelo").json()
    assert corpo["dados"] == "sinteticos"
    assert corpo["regra"] == "pontuacao-v2"
    assert corpo["acuracia"] >= 0.8
    assert corpo["f1_macro"] >= 0.8


def test_treina_nova_versao_com_os_parametros_pedidos():
    with TestClient(app) as cliente:
        antes = cliente.get("/modelo").json()
        resposta = cliente.post("/treinamentos", json={"semente": 7, "amostras": 5000})
        depois = cliente.get("/modelo").json()
        classificacao = cliente.post("/predict", json=SENIOR).json()
    corpo = resposta.json()
    assert resposta.status_code == 200
    assert corpo["semente"] == 7
    assert corpo["amostras_de_treino"] + corpo["amostras_de_teste"] == 5000
    assert corpo["acuracia"] >= 0.8
    assert depois["versao"] == corpo["versao"] != antes["versao"]
    assert classificacao["modelo"] == "regressao-logistica v" + corpo["versao"]


def test_treinamento_sem_semente_usa_uma_semente_gerada():
    with TestClient(app) as cliente:
        corpo = cliente.post("/treinamentos", json={}).json()
    assert corpo["semente"] >= 0
    assert corpo["amostras_de_treino"] + corpo["amostras_de_teste"] == 3000


def test_recusa_treinamento_com_poucas_amostras():
    with TestClient(app) as cliente:
        resposta = cliente.post("/treinamentos", json={"amostras": 10})
    assert resposta.status_code == 422
