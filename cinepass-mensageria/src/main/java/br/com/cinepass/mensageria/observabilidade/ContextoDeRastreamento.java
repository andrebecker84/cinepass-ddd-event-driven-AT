package br.com.cinepass.mensageria.observabilidade;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Atravessa o contexto de rastreamento por uma fronteira que o Micrometer não enxerga
 * sozinho: a tabela do outbox.
 *
 * <p>Na requisição HTTP e no Kafka a propagação é automática. Entre a gravação do evento
 * e a publicação pelo relay, porém, há uma linha de banco e outra thread. {@link #capturar()}
 * serializa o contexto corrente nos mesmos cabeçalhos que iriam pela rede
 * ({@code traceparent}); {@link #continuar} o reabre do outro lado, como filho do span original.
 */
public class ContextoDeRastreamento {

    private final Tracer tracer;
    private final Propagator propagator;

    public ContextoDeRastreamento(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Map<String, String> capturar() {
        Map<String, String> cabecalhos = new LinkedHashMap<>();
        if (tracer == null || propagator == null) {
            return cabecalhos;
        }
        TraceContext contexto = tracer.currentTraceContext().context();
        if (contexto != null) {
            propagator.inject(contexto, cabecalhos, Map::put);
        }
        return cabecalhos;
    }

    public SpanAberto continuar(String nome, Map<String, String> cabecalhos) {
        if (tracer == null || propagator == null) {
            return SpanAberto.NENHUM;
        }
        Span span = propagator.extract(cabecalhos, Map::get).name(nome).start();
        return new SpanAberto(span, tracer.withSpan(span));
    }

    /** Span em escopo: fechar encerra o escopo e o span, nessa ordem. */
    public record SpanAberto(Span span, Tracer.SpanInScope escopo) implements AutoCloseable {

        static final SpanAberto NENHUM = new SpanAberto(null, null);

        public void tag(String chave, String valor) {
            if (span != null && valor != null) span.tag(chave, valor);
        }

        public void erro(Throwable causa) {
            if (span != null) span.error(causa);
        }

        @Override
        public void close() {
            if (escopo != null) escopo.close();
            if (span != null) span.end();
        }
    }
}
