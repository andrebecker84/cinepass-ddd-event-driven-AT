package br.com.cinepass.notificacao.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Contato de um cliente para notificações. O cadastro pertence a este serviço. */
public record Destinatario(UUID clienteId, String nome, Email email) {
    public Destinatario {
        Objects.requireNonNull(clienteId, "O cliente é obrigatório.");
        Objects.requireNonNull(nome, "O nome é obrigatório.");
        Objects.requireNonNull(email, "O e-mail é obrigatório.");
        if (nome.isBlank()) throw new IllegalArgumentException("O nome é obrigatório.");
    }
}
