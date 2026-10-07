package br.com.cinepass.reserva.domain.model;

import java.util.Objects;

public record AssentoId(String valor) {
    public AssentoId {
        Objects.requireNonNull(valor, "O assento é obrigatório.");
        valor = valor.trim().toUpperCase();
        if (valor.isBlank()) throw new IllegalArgumentException("O assento não pode ser vazio.");
    }
}
