package br.com.cinepass.gateway;

import br.com.cinepass.mensageria.correlacao.Correlacao;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Ponto de origem da correlação. Toda requisição externa sai do Gateway com um
 * {@code X-Correlation-Id}: o que o cliente mandou, se tiver formato válido, ou um novo.
 * O mesmo valor volta na resposta, junto com o {@code X-Trace-Id} do Zipkin, para que
 * quem chamou consiga achar a operação nos logs e no trace.
 *
 * <p>Daqui em diante o identificador segue sozinho: cabeçalho HTTP até o reserva-service,
 * envelope e cabeçalho Kafka até os consumidores.
 */
@Component
public class CorrelacaoGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelacaoGlobalFilter.class);
    static final String HEADER_TRACE_ID = "X-Trace-Id";

    private final ObjectProvider<Tracer> tracer;

    public CorrelacaoGlobalFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String recebido = exchange.getRequest().getHeaders().getFirst(Correlacao.HEADER);
        String correlationId = Correlacao.validarOuGerar(recebido);
        ServerHttpRequest requisicao = exchange.getRequest().mutate()
                .headers(cabecalhos -> cabecalhos.set(Correlacao.HEADER, correlationId))
                .build();

        Tracer t = tracer.getIfAvailable();
        Span span = t == null ? null : t.currentSpan();
        String traceId = span == null ? null : span.context().traceId();
        // Gravado na hora de enviar a resposta, com set: o serviço de destino devolve o mesmo
        // cabeçalho, e um add aqui o deixaria duplicado.
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(Correlacao.HEADER, correlationId);
            if (traceId != null) {
                exchange.getResponse().getHeaders().set(HEADER_TRACE_ID, traceId);
            }
            return Mono.empty();
        });

        String metodo = requisicao.getMethod().name();
        String caminho = requisicao.getPath().value();
        long inicio = System.nanoTime();
        comCorrelacao(correlationId, () -> log.info("Requisição recebida: {} {} correlationIdGerado={}",
                metodo, caminho, !correlationId.equals(recebido)));

        return chain.filter(exchange.mutate().request(requisicao).build())
                .doFinally(sinal -> comCorrelacao(correlationId, () -> log.info(
                        "Resposta enviada: {} {} status={} duracaoMs={}", metodo, caminho,
                        exchange.getResponse().getStatusCode(),
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio))));
    }

    /** No mundo reativo a thread muda; o MDC é aplicado só em volta da linha de log. */
    private static void comCorrelacao(String correlationId, Runnable registro) {
        MDC.put(Correlacao.MDC_CORRELATION_ID, correlationId);
        try {
            registro.run();
        } finally {
            MDC.remove(Correlacao.MDC_CORRELATION_ID);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
