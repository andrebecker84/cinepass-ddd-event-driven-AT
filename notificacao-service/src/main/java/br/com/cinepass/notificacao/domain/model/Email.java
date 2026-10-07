package br.com.cinepass.notificacao.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * E-mail de contato. É dado pessoal: o valor completo fica no banco do serviço e nunca vai
 * para log. Quem precisa mostrar o destinatário usa {@link #mascarado()}.
 */
public record Email(String valor) {
    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(valor, "O e-mail é obrigatório.");
        valor = valor.trim().toLowerCase();
        if (!FORMATO.matcher(valor).matches()) throw new IllegalArgumentException("E-mail inválido.");
    }

    /** {@code maria.silva@example.com} vira {@code m***@example.com}. */
    public String mascarado() {
        int arroba = valor.indexOf('@');
        return valor.charAt(0) + "***" + valor.substring(arroba);
    }

    /** Mascarado também aqui: um log descuidado que imprima o objeto não vaza o endereço. */
    @Override
    public String toString() {
        return mascarado();
    }
}
