package br.com.cinepass.mensageria;

/**
 * Nomes dos tópicos Kafka. Nome errado não quebra o build nem lança exceção: cria um
 * tópico novo que ninguém lê. Por isso ficam em constantes, num lugar só.
 */
public final class Topicos {

    /** Ciclo de vida da reserva. Um tópico só para todos os tipos: é o que garante a ordem por reserva. */
    public static final String RESERVA_EVENTOS = "cinepass.reserva.eventos";

    /** Partições do tópico principal e das DLTs. Teto da concorrência de cada consumidor. */
    public static final int PARTICOES = 3;

    /** Retenção do tópico principal: 7 dias, o bastante para um consumidor novo reler a semana. */
    public static final long RETENCAO_MS = 7L * 24 * 60 * 60 * 1000;

    private Topicos() {
    }

    /**
     * DLT própria de cada consumidor. Uma DLT compartilhada misturaria as falhas da
     * notificação com as da auditoria, e o reprocessamento de uma reentregaria à outra.
     */
    public static String dltDe(String topico, String consumidor) {
        return topico + "." + consumidor + ".DLT";
    }
}
