package br.com.cinepass.notificacao.infrastructure.persistence;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "destinatarios")
public class DestinatarioJpaEntity {
    @Id @Column(name = "cliente_id") private UUID clienteId;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, length = 160) private String email;

    protected DestinatarioJpaEntity() {}

    public DestinatarioJpaEntity(UUID clienteId, String nome, String email) {
        this.clienteId = clienteId; this.nome = nome; this.email = email;
    }

    public UUID getClienteId() { return clienteId; } public String getNome() { return nome; } public String getEmail() { return email; }
}
