package br.com.cinepass.pagamento.application;

import java.util.UUID;

public class PagamentoNaoEncontradoException extends RuntimeException {
    public PagamentoNaoEncontradoException(UUID id) {
        super("Pagamento não encontrado: " + id);
    }
}
