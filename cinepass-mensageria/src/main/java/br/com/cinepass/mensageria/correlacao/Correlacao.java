package br.com.cinepass.mensageria.correlacao;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identificador de correlação de uma operação iniciada externamente. Nasce no API Gateway,
 * viaja no cabeçalho HTTP {@value #HEADER} e, depois, no envelope e nos cabeçalhos do Kafka.
 *
 * <p>Fica no MDC do SLF4J, e é de lá que os logs (console e Logstash) o leem. As chaves
 * do MDC são as mesmas em todos os serviços, o que permite filtrar no Kibana.
 */
public final class Correlacao {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_RESERVA_ID = "reservaId";
    public static final String MDC_EVENT_ID = "eventId";
    public static final String MDC_EVENT_TYPE = "eventType";

    /** Aceita UUID e identificadores simples; qualquer outra coisa é descartada e substituída. */
    private static final Pattern FORMATO_VALIDO = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    private Correlacao() {
    }

    /** Valor recebido de fora vira log e cabeçalho: só passa se tiver formato seguro. */
    public static String validarOuGerar(String recebido) {
        if (recebido != null && FORMATO_VALIDO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }

    public static String atual() {
        return MDC.get(MDC_CORRELATION_ID);
    }

    /** O correlationId da operação corrente, ou um novo se a execução não veio de uma requisição. */
    public static String atualOuNovo() {
        String atual = atual();
        return atual != null ? atual : UUID.randomUUID().toString();
    }
}
