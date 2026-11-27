import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from types import SimpleNamespace

import pytest

import worker
from processamento import (AGUARDANDO_ML, CONCLUIDO, EM_TREINAMENTO, FALHOU, ClienteDoMl, MlIndisponivel, Pedido,
                           TreinamentoRecusado, processar)

METADADOS = {"versao": "20261124120000123", "semente": 7, "amostras_de_treino": 4000, "amostras_de_teste": 1000,
             "acuracia": 0.95, "f1_macro": 0.94}
PEDIDO = Pedido(1, 5000, 7)


class MlFalso:
    def __init__(self, erro_na_verificacao=None, erro_no_treino=None):
        self.erro_na_verificacao = erro_na_verificacao
        self.erro_no_treino = erro_no_treino
        self.treinos = []

    def verificar(self):
        if self.erro_na_verificacao:
            raise self.erro_na_verificacao

    def treinar(self, pedido):
        self.treinos.append(pedido)
        if self.erro_no_treino:
            raise self.erro_no_treino
        return METADADOS


def executar(ml, tentativa=1, maximo=3):
    eventos, reagendados = [], []
    status = processar(PEDIDO, tentativa, maximo, 10, ml, eventos.append,
                       lambda pedido, proxima: reagendados.append((pedido, proxima)))
    return status, eventos, reagendados


def test_treina_e_publica_o_andamento_ate_concluir():
    ml = MlFalso()
    status, eventos, reagendados = executar(ml)
    assert status == CONCLUIDO
    assert [e["status"] for e in eventos] == [EM_TREINAMENTO, CONCLUIDO]
    assert eventos[1]["resultado"] == {"versao": "20261124120000123", "semente": 7, "amostrasDeTreino": 4000,
                                       "amostrasDeTeste": 1000, "acuracia": 0.95, "f1Macro": 0.94}
    assert eventos[1]["detalhe"] == "Modelo v20261124120000123 publicado com acuracia 0.95"
    assert ml.treinos == [PEDIDO]
    assert reagendados == []


def test_ml_fora_do_ar_reagenda_sem_perder_o_pedido():
    status, eventos, reagendados = executar(MlFalso(erro_na_verificacao=MlIndisponivel("Servico de ML fora do ar")))
    assert status == AGUARDANDO_ML
    assert eventos[0]["status"] == AGUARDANDO_ML
    assert eventos[0]["detalhe"] == "Servico de ML fora do ar. Nova tentativa em 10 s (tentativa 1 de 3)"
    assert reagendados == [(PEDIDO, 2)]


def test_ml_cai_durante_o_treino_e_o_pedido_volta_para_a_espera():
    status, eventos, reagendados = executar(MlFalso(erro_no_treino=MlIndisponivel("Servico de ML respondeu 503")),
                                            tentativa=2)
    assert status == AGUARDANDO_ML
    assert [e["status"] for e in eventos] == [EM_TREINAMENTO, AGUARDANDO_ML]
    assert reagendados == [(PEDIDO, 3)]


def test_desiste_depois_do_maximo_de_tentativas():
    status, eventos, reagendados = executar(MlFalso(erro_na_verificacao=MlIndisponivel("x")), tentativa=3)
    assert status == FALHOU
    assert eventos[0]["detalhe"] == "Servico de ML indisponivel apos 3 tentativas"
    assert reagendados == []


def test_pedido_recusado_pelo_ml_falha_sem_nova_tentativa():
    erro = TreinamentoRecusado("Servico de ML recusou o pedido (HTTP 422)")
    status, eventos, reagendados = executar(MlFalso(erro_no_treino=erro))
    assert status == FALHOU
    assert eventos[-1]["detalhe"] == "Servico de ML recusou o pedido (HTTP 422)"
    assert reagendados == []


def test_pedido_ida_e_volta_em_json():
    pedido = Pedido.de_json(b'{"solicitacaoId": 4, "amostras": null, "semente": null}')
    assert pedido == Pedido(4, 3000, None)
    assert Pedido.de_json(PEDIDO.para_json()) == PEDIDO


class MlDeTeste(BaseHTTPRequestHandler):
    def log_message(self, *argumentos):
        pass

    def responder(self, codigo, corpo):
        dados = json.dumps(corpo).encode("utf-8")
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(dados)))
        self.end_headers()
        self.wfile.write(dados)

    def do_GET(self):
        self.responder(200, {"status": "ok"})

    def do_POST(self):
        pedido = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if pedido["amostras"] == 1:
            self.responder(422, {"detail": "amostras"})
        elif pedido["amostras"] == 2:
            self.responder(503, {"detail": "ocupado"})
        else:
            self.responder(200, dict(METADADOS, semente=pedido["semente"]))


@pytest.fixture
def servidor():
    http = HTTPServer(("127.0.0.1", 0), MlDeTeste)
    threading.Thread(target=http.serve_forever, daemon=True).start()
    yield "http://127.0.0.1:" + str(http.server_port) + "/"
    http.shutdown()


def test_cliente_http_chama_o_servico_de_ml(servidor):
    ml = ClienteDoMl(servidor, 5)
    ml.verificar()
    assert ml.treinar(Pedido(1, 5000, 9))["semente"] == 9
    with pytest.raises(TreinamentoRecusado):
        ml.treinar(Pedido(1, 1, 9))
    with pytest.raises(MlIndisponivel):
        ml.treinar(Pedido(1, 2, 9))


def test_cliente_http_trata_servico_fora_do_ar():
    with pytest.raises(MlIndisponivel):
        ClienteDoMl("http://127.0.0.1:9", 1).verificar()


def test_filas_e_tentativa_da_mensagem():
    declaradas = {}
    canal = SimpleNamespace(queue_declare=lambda nome, durable, arguments=None: declaradas.update({nome: arguments}))
    worker.declarar_filas(canal)
    assert declaradas["treinamentos.solicitados"] is None
    assert declaradas["treinamentos.espera"]["x-dead-letter-routing-key"] == "treinamentos.solicitados"
    assert declaradas["treinamentos.espera"]["x-message-ttl"] == 10000
    assert worker.tentativa_da_mensagem(SimpleNamespace(headers=None)) == 1
    assert worker.tentativa_da_mensagem(SimpleNamespace(headers={"x-tentativa": 4})) == 4
