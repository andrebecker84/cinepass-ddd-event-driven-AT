package br.com.cinepass.ingresso.domain.model;

import java.util.Objects;
import java.util.UUID;

public record IngressoId(UUID valor) {
    public IngressoId { Objects.requireNonNull(valor, "O id do ingresso é obrigatório."); }
    public static IngressoId novo() { return new IngressoId(UUID.randomUUID()); }
}
