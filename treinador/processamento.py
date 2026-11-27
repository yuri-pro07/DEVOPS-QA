import json
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional

AGUARDANDO_ML = "AGUARDANDO_ML"
EM_TREINAMENTO = "EM_TREINAMENTO"
CONCLUIDO = "CONCLUIDO"
FALHOU = "FALHOU"
AMOSTRAS_PADRAO = 3000


class MlIndisponivel(Exception):
    pass


class TreinamentoRecusado(Exception):
    pass


@dataclass(frozen=True)
class Pedido:
    solicitacao_id: int
    amostras: int
    semente: Optional[int]

    @staticmethod
    def de_json(corpo):
        dados = json.loads(corpo)
        semente = dados.get("semente")
        return Pedido(int(dados["solicitacaoId"]), int(dados.get("amostras") or AMOSTRAS_PADRAO),
                      None if semente is None else int(semente))

    def para_json(self):
        return json.dumps({"solicitacaoId": self.solicitacao_id, "amostras": self.amostras, "semente": self.semente})


def evento(solicitacao_id, status, detalhe, tentativa, resultado=None):
    return {
        "solicitacaoId": solicitacao_id,
        "status": status,
        "detalhe": detalhe,
        "tentativa": tentativa,
        "resultado": resultado,
    }


def resultado_do_modelo(metadados):
    return {
        "versao": metadados["versao"],
        "semente": metadados["semente"],
        "amostrasDeTreino": metadados["amostras_de_treino"],
        "amostrasDeTeste": metadados["amostras_de_teste"],
        "acuracia": metadados["acuracia"],
        "f1Macro": metadados["f1_macro"],
    }


class ClienteDoMl:
    def __init__(self, url, tempo_de_treino):
        self.url = url.rstrip("/")
        self.tempo_de_treino = tempo_de_treino

    def _chamar(self, requisicao, tempo):
        try:
            with urllib.request.urlopen(requisicao, timeout=tempo) as resposta:
                return json.loads(resposta.read().decode("utf-8"))
        except urllib.error.HTTPError as erro:
            if erro.code >= 500:
                raise MlIndisponivel("Servico de ML respondeu " + str(erro.code)) from erro
            raise TreinamentoRecusado("Servico de ML recusou o pedido (HTTP " + str(erro.code) + ")") from erro
        except (urllib.error.URLError, OSError) as erro:
            raise MlIndisponivel("Servico de ML fora do ar") from erro

    def verificar(self):
        self._chamar(urllib.request.Request(self.url + "/health"), 3)

    def treinar(self, pedido):
        corpo = json.dumps({"amostras": pedido.amostras, "semente": pedido.semente}).encode("utf-8")
        requisicao = urllib.request.Request(self.url + "/treinamentos", data=corpo, method="POST",
                                            headers={"Content-Type": "application/json"})
        return self._chamar(requisicao, self.tempo_de_treino)


def processar(pedido, tentativa, maximo_de_tentativas, espera_em_segundos, ml, publicar, reagendar):
    try:
        ml.verificar()
        publicar(evento(pedido.solicitacao_id, EM_TREINAMENTO,
                        "Servico de ML disponivel. Treinando com " + str(pedido.amostras) + " amostras", tentativa))
        metadados = ml.treinar(pedido)
    except MlIndisponivel as erro:
        if tentativa >= maximo_de_tentativas:
            publicar(evento(pedido.solicitacao_id, FALHOU,
                            "Servico de ML indisponivel apos " + str(tentativa) + " tentativas", tentativa))
            return FALHOU
        publicar(evento(pedido.solicitacao_id, AGUARDANDO_ML,
                        str(erro) + ". Nova tentativa em " + str(espera_em_segundos) + " s (tentativa "
                        + str(tentativa) + " de " + str(maximo_de_tentativas) + ")", tentativa))
        reagendar(pedido, tentativa + 1)
        return AGUARDANDO_ML
    except TreinamentoRecusado as erro:
        publicar(evento(pedido.solicitacao_id, FALHOU, str(erro), tentativa))
        return FALHOU
    resultado = resultado_do_modelo(metadados)
    publicar(evento(pedido.solicitacao_id, CONCLUIDO,
                    "Modelo v" + resultado["versao"] + " publicado com acuracia " + str(resultado["acuracia"]),
                    tentativa, resultado))
    return CONCLUIDO
