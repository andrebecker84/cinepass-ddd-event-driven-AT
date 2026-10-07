package br.com.cinepass.reputacao.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reputacoes")
public class ReputacaoJpaEntity {
    @Id @Column(name = "cliente_id") private UUID clienteId;
    @Column(name = "reservas_confirmadas", nullable = false) private int reservasConfirmadas;
    @Column(name = "reservas_canceladas", nullable = false) private int reservasCanceladas;
    @Column(name = "pagamentos_recusados", nullable = false) private int pagamentosRecusados;
    @Column(nullable = false) private long pontos;
    @Column(name = "valor_total_gasto", nullable = false, precision = 14, scale = 2) private BigDecimal valorTotalGasto;
    @Column(nullable = false, length = 10) private String nivel;
    @Column(name = "atualizada_em", nullable = false) private Instant atualizadaEm;

    protected ReputacaoJpaEntity() {}

    void atualizar(int confirmadas, int canceladas, int recusados, long pontos, BigDecimal valorTotalGasto, String nivel, Instant atualizadaEm) {
        this.reservasConfirmadas = confirmadas; this.reservasCanceladas = canceladas; this.pagamentosRecusados = recusados;
        this.pontos = pontos; this.valorTotalGasto = valorTotalGasto; this.nivel = nivel; this.atualizadaEm = atualizadaEm;
    }

    public UUID getClienteId() { return clienteId; } public int getReservasConfirmadas() { return reservasConfirmadas; }
    public int getReservasCanceladas() { return reservasCanceladas; } public int getPagamentosRecusados() { return pagamentosRecusados; }
    public long getPontos() { return pontos; } public BigDecimal getValorTotalGasto() { return valorTotalGasto; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
