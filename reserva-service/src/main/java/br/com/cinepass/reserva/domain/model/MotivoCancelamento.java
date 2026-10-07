package br.com.cinepass.reserva.domain.model;

public enum MotivoCancelamento {
    /** O pagamento-service respondeu, e recusou a cobrança. */
    PAGAMENTO_RECUSADO,
    /** O pagamento-service não respondeu; nenhuma cobrança foi confirmada. */
    FALHA_COMUNICACAO_PAGAMENTO,
    /** O pagamento foi aprovado, mas o ingresso não pôde ser emitido: houve compensação. */
    FALHA_EMISSAO_INGRESSO
}
