package br.com.cinepass.reserva.domain.model;

import java.util.Objects;
import java.util.UUID;

public record FilmeId(UUID valor) {
    public FilmeId { Objects.requireNonNull(valor, "O id do filme é obrigatório."); }
}
