package br.com.cinepass.pagamento.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PagamentoId(UUID valor) {
    public PagamentoId {
        Objects.requireNonNull(valor, "O id do pagamento é obrigatório.");
    }

    public static PagamentoId novo() {
        return new PagamentoId(UUID.randomUUID());
    }
}
