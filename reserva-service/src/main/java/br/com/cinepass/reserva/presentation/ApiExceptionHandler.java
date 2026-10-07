package br.com.cinepass.reserva.presentation;

import br.com.cinepass.reserva.application.*;
import br.com.cinepass.reserva.domain.model.AssentoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({ReservaNaoEncontradaException.class, SessaoNaoEncontradaException.class})
    ProblemDetail naoEncontrado(RuntimeException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()); p.setTitle("Recurso não encontrado"); return p;
    }

    @ExceptionHandler({AssentoIndisponivelException.class, IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail negocio(RuntimeException ex){
        log.warn("Regra de negócio violada: {}", ex.getMessage());
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); p.setTitle("Regra de negócio violada"); return p;
    }

    @ExceptionHandler(PagamentoRecusadoException.class)
    ProblemDetail pagamentoRecusado(PagamentoRecusadoException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        p.setTitle("Pagamento recusado"); p.setProperty("reservaId", ex.getReservaId()); p.setProperty("status", "CANCELADA"); return p;
    }

    /** A Saga compensou: o estado é consistente, a reserva só não foi concluída. */
    @ExceptionHandler(ReservaCanceladaException.class)
    ProblemDetail reservaCancelada(ReservaCanceladaException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        p.setTitle("Reserva cancelada pela Saga");
        p.setProperty("reservaId", ex.getReservaId());
        p.setProperty("pagamentoId", ex.getPagamentoId());
        p.setProperty("motivo", ex.getMotivo());
        p.setProperty("pagamentoEstornado", ex.isPagamentoEstornado());
        return p;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail conflito(OptimisticLockingFailureException ex){
        log.warn("Conflito de concorrência persistente: {}", ex.getMessage());
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A sessão foi alterada por outra reserva ao mesmo tempo. Tente novamente.");
        p.setTitle("Conflito de concorrência"); return p;
    }

    @ExceptionHandler(FalhaIntegracaoException.class)
    ProblemDetail integracao(FalhaIntegracaoException ex){
        var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage()); p.setTitle("Falha de integração"); return p;
    }
}
