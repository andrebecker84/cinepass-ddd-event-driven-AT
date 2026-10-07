package br.com.cinepass.pagamento.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReservaId(UUID valor) {
    public ReservaId {
        Objects.requireNonNull(valor, "O id da reserva é obrigatório.");
    }
}
