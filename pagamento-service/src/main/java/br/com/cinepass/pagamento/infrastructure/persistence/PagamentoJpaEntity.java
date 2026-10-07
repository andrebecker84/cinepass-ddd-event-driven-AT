package br.com.cinepass.pagamento.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagamentos", indexes = @Index(name = "idx_pagamento_reserva", columnList = "reserva_id"))
public class PagamentoJpaEntity {
    @Id
    private UUID id;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected PagamentoJpaEntity() {}

    public PagamentoJpaEntity(UUID id, UUID reservaId, BigDecimal valor, String status, Instant criadoEm) {
        this.id = id; this.reservaId = reservaId; this.valor = valor; this.status = status; this.criadoEm = criadoEm;
    }

    public UUID getId() { return id; }
    public UUID getReservaId() { return reservaId; }
    public BigDecimal getValor() { return valor; }
    public String getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
}
