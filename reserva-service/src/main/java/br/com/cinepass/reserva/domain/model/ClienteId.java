package br.com.cinepass.reserva.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ClienteId(UUID valor) {
    public ClienteId { Objects.requireNonNull(valor, "O id do cliente é obrigatório."); }
}
