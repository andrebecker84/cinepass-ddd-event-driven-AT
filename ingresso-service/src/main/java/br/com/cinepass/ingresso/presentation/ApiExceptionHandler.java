package br.com.cinepass.ingresso.presentation;

import br.com.cinepass.ingresso.application.EmissaoIngressoException;
import br.com.cinepass.ingresso.application.IngressoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IngressoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(IngressoNaoEncontradoException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()); p.setTitle("Ingresso não encontrado"); return p;
    }
    @ExceptionHandler(EmissaoIngressoException.class)
    ProblemDetail falhaEmissao(EmissaoIngressoException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()); p.setTitle("Falha na emissão"); return p;
    }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail regra(RuntimeException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); p.setTitle("Regra de negócio violada"); return p;
    }
}
