package br.edu.gamificacaocursos.treinamento.domain;

public enum StatusDoTreinamento {
    RECEBIDA(5, "Solicitacao registrada. Aguardando o envio para a fila"),
    NA_FILA(15, "Na fila de treinamentos. Aguardando o worker"),
    AGUARDANDO_ML(25, "Servico de ML indisponivel. A solicitacao continua guardada e sera tentada de novo"),
    EM_TREINAMENTO(60, "Modelo sendo treinado pelo servico de ML"),
    CONCLUIDO(100, "Treinamento concluido e nova versao do modelo publicada"),
    FALHOU(100, "Treinamento encerrado sem sucesso");

    private final int percentual;
    private final String descricao;

    StatusDoTreinamento(int percentual, String descricao) {
        this.percentual = percentual;
        this.descricao = descricao;
    }

    public int percentual() {
        return percentual;
    }

    public String descricao() {
        return descricao;
    }

    public boolean finalizado() {
        return this == CONCLUIDO || this == FALHOU;
    }

    public boolean informadoPeloWorker() {
        return this == AGUARDANDO_ML || this == EM_TREINAMENTO || finalizado();
    }
}
