package br.com.cinepass.reserva.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor) {
    public Dinheiro {
        Objects.requireNonNull(valor, "O valor é obrigatório.");
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() < 0) throw new IllegalArgumentException("O valor não pode ser negativo.");
    }
    public Dinheiro multiplicar(int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }
}
