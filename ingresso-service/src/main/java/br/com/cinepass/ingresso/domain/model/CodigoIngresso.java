package br.com.cinepass.ingresso.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CodigoIngresso(String valor) {
    public CodigoIngresso {
        Objects.requireNonNull(valor, "O código é obrigatório.");
        if (valor.isBlank()) throw new IllegalArgumentException("O código não pode ser vazio.");
    }
    public static CodigoIngresso gerar() {
        return new CodigoIngresso("CINE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
