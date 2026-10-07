package br.com.cinepass.reserva.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SessaoId(UUID valor) {
    public SessaoId { Objects.requireNonNull(valor, "O id da sessão é obrigatório."); }
}
