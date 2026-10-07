package br.com.cinepass.mensageria.correlacao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o {@code X-Correlation-Id} que o API Gateway gerou e o coloca no MDC durante toda a
 * requisição. Se o serviço for chamado direto, sem o Gateway, gera um novo, para que
 * nenhum log fique sem correlação.
 */
public class CorrelacaoFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = Correlacao.validarOuGerar(request.getHeader(Correlacao.HEADER));
        MDC.put(Correlacao.MDC_CORRELATION_ID, correlationId);
        response.setHeader(Correlacao.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(Correlacao.MDC_CORRELATION_ID);
        }
    }
}
