from fastapi.testclient import TestClient

from app import app, carregar_trechos, PASTA_DOS_DOCUMENTOS

cliente = TestClient(app)


def test_servico_carrega_todos_os_documentos():
    resposta = cliente.get("/health")
    assert resposta.status_code == 200
    assert resposta.json()["documentos"] == 7
    assert resposta.json()["trechos"] == len(carregar_trechos(PASTA_DOS_DOCUMENTOS))


def test_lista_documentos_com_secoes():
    documentos = {d["fonte"]: d for d in cliente.get("/documentos").json()}
    assert "Cursos da trilha intermediaria" in documentos["trilha-intermediaria.md"]["secoes"]


def test_busca_por_mensageria_traz_o_trecho_certo_primeiro():
    trechos = cliente.get("/buscar", params={"q": "mensageria filas assincrono"}).json()["trechos"]
    assert trechos[0]["id"] == "trilha-intermediaria#3"
    assert "Mensageria com RabbitMQ" in trechos[0]["texto"]


def test_busca_ignora_acentos():
    sem_acento = cliente.get("/buscar", params={"q": "trilha intermediaria"}).json()["trechos"]
    com_acento = cliente.get("/buscar", params={"q": "trilha intermediária"}).json()["trechos"]
    assert [t["id"] for t in sem_acento] == [t["id"] for t in com_acento]
    assert sem_acento[0]["fonte"] == "trilha-intermediaria.md"


def test_resultados_vem_ordenados_por_relevancia_e_respeitam_k():
    trechos = cliente.get("/buscar", params={"q": "cursos trilha avancada arquitetura", "k": 3}).json()["trechos"]
    assert len(trechos) == 3
    relevancias = [t["relevancia"] for t in trechos]
    assert relevancias == sorted(relevancias, reverse=True)


def test_consulta_sem_relacao_nao_traz_trechos():
    assert cliente.get("/buscar", params={"q": "xyzw kkkk"}).json()["trechos"] == []


def test_consulta_invalida_e_recusada():
    assert cliente.get("/buscar", params={"q": "a"}).status_code == 422
    assert cliente.get("/buscar", params={"q": "java", "k": 11}).status_code == 422
