package br.com.cinepass.mensageria.inbox;

import br.com.cinepass.mensageria.CabecalhosKafka;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Leitura dos cabeçalhos de um registro, para log e tracing fora do fluxo normal (falhas e DLT). */
final class CabecalhosDoRegistro {

    private static final Set<String> RASTREAMENTO = Set.of("traceparent", "tracestate", "b3");

    private CabecalhosDoRegistro() {
    }

    static String texto(ConsumerRecord<?, ?> registro, String nome) {
        Header cabecalho = registro.headers().lastHeader(nome);
        return cabecalho == null ? null : new String(cabecalho.value(), StandardCharsets.UTF_8);
    }

    static MdcDoEvento mdc(ConsumerRecord<?, ?> registro) {
        return MdcDoEvento.de(uuid(texto(registro, CabecalhosKafka.RESERVA_ID)), uuid(texto(registro, CabecalhosKafka.EVENT_ID)),
                texto(registro, CabecalhosKafka.EVENT_TYPE), texto(registro, CabecalhosKafka.CORRELATION_ID));
    }

    static Map<String, String> rastreamento(ConsumerRecord<?, ?> registro) {
        Map<String, String> mapa = new HashMap<>();
        for (Header cabecalho : registro.headers()) {
            if (RASTREAMENTO.contains(cabecalho.key())) {
                mapa.put(cabecalho.key(), new String(cabecalho.value(), StandardCharsets.UTF_8));
            }
        }
        return mapa;
    }

    private static UUID uuid(String valor) {
        try {
            return valor == null ? null : UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
