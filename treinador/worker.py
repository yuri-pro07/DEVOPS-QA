import json
import logging
import os
import time

import pika

from processamento import ClienteDoMl, Pedido, processar

FILA_DE_SOLICITACOES = os.getenv("FILA_DE_SOLICITACOES", "treinamentos.solicitados")
FILA_DE_ESPERA = os.getenv("FILA_DE_ESPERA", "treinamentos.espera")
FILA_DE_ANDAMENTO = os.getenv("FILA_DE_ANDAMENTO", "treinamentos.andamento")
ESPERA_EM_SEGUNDOS = int(os.getenv("ESPERA_EM_SEGUNDOS", "10"))
MAXIMO_DE_TENTATIVAS = int(os.getenv("MAXIMO_DE_TENTATIVAS", "60"))
CABECALHO_DE_TENTATIVA = "x-tentativa"

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("treinador")
logging.getLogger("pika").setLevel(logging.CRITICAL)


def declarar_filas(canal):
    canal.queue_declare(FILA_DE_SOLICITACOES, durable=True)
    canal.queue_declare(FILA_DE_ANDAMENTO, durable=True)
    canal.queue_declare(FILA_DE_ESPERA, durable=True, arguments={
        "x-message-ttl": ESPERA_EM_SEGUNDOS * 1000,
        "x-dead-letter-exchange": "",
        "x-dead-letter-routing-key": FILA_DE_SOLICITACOES,
    })


def propriedades(cabecalhos=None):
    return pika.BasicProperties(content_type="application/json", delivery_mode=2, headers=cabecalhos)


def tentativa_da_mensagem(props):
    return int((props.headers or {}).get(CABECALHO_DE_TENTATIVA, 1))


def consumir(canal, ml):
    def publicar(evento):
        canal.basic_publish("", FILA_DE_ANDAMENTO, json.dumps(evento).encode("utf-8"), propriedades())
        log.info("solicitacao %s: %s - %s", evento["solicitacaoId"], evento["status"], evento["detalhe"])

    def reagendar(pedido, tentativa):
        canal.basic_publish("", FILA_DE_ESPERA, pedido.para_json().encode("utf-8"),
                            propriedades({CABECALHO_DE_TENTATIVA: tentativa}))

    def ao_receber(canal_atual, entrega, props, corpo):
        try:
            pedido = Pedido.de_json(corpo)
        except (ValueError, KeyError, TypeError):
            log.error("mensagem invalida descartada: %r", corpo[:200])
            canal_atual.basic_ack(entrega.delivery_tag)
            return
        processar(pedido, tentativa_da_mensagem(props), MAXIMO_DE_TENTATIVAS, ESPERA_EM_SEGUNDOS, ml,
                  publicar, reagendar)
        canal_atual.basic_ack(entrega.delivery_tag)

    canal.basic_qos(prefetch_count=1)
    canal.basic_consume(FILA_DE_SOLICITACOES, ao_receber)
    canal.start_consuming()


def main():
    parametros = pika.URLParameters(os.getenv("RABBITMQ_URL", "amqp://guest:guest@localhost:5672/%2F"))
    ml = ClienteDoMl(os.getenv("ML_URL", "http://localhost:8001"), int(os.getenv("TEMPO_DE_TREINO", "120")))
    while True:
        try:
            with pika.BlockingConnection(parametros) as conexao:
                canal = conexao.channel()
                declarar_filas(canal)
                log.info("aguardando solicitacoes na fila %s", FILA_DE_SOLICITACOES)
                consumir(canal, ml)
        except pika.exceptions.AMQPError as erro:
            log.warning("conexao com o RabbitMQ indisponivel (%s), nova tentativa em 5 s", erro.__class__.__name__)
            time.sleep(5)


if __name__ == "__main__":
    main()
