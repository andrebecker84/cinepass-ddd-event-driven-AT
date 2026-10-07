package br.com.cinepass.reserva.domain.model;

import java.util.Objects;

public record Filme(FilmeId id, String titulo, int duracaoMinutos, String classificacao) {
    public Filme {
        Objects.requireNonNull(id); Objects.requireNonNull(titulo); Objects.requireNonNull(classificacao);
        if (titulo.isBlank()) throw new IllegalArgumentException("O título é obrigatório.");
        if (duracaoMinutos <= 0) throw new IllegalArgumentException("A duração deve ser positiva.");
    }
}
