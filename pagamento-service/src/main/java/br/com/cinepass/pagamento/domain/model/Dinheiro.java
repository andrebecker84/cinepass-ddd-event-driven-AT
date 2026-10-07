package br.com.cinepass.pagamento.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor) {
    public Dinheiro {
        Objects.requireNonNull(valor, "O valor é obrigatório.");
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
    }
}
