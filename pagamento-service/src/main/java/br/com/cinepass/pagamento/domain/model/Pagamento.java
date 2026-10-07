package br.com.cinepass.pagamento.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Pagamento {
    private final PagamentoId id;
    private final ReservaId reservaId;
    private final Dinheiro valor;
    private StatusPagamento status;
    private final Instant criadoEm;

    private Pagamento(PagamentoId id, ReservaId reservaId, Dinheiro valor,
                      StatusPagamento status, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.reservaId = Objects.requireNonNull(reservaId);
        this.valor = Objects.requireNonNull(valor);
        this.status = Objects.requireNonNull(status);
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    public static Pagamento solicitar(ReservaId reservaId, Dinheiro valor) {
        return new Pagamento(PagamentoId.novo(), reservaId, valor, StatusPagamento.PENDENTE, Instant.now());
    }

    public static Pagamento restaurar(PagamentoId id, ReservaId reservaId, Dinheiro valor,
                                      StatusPagamento status, Instant criadoEm) {
        return new Pagamento(id, reservaId, valor, status, criadoEm);
    }

    public void aprovar() {
        exigirPendente();
        status = StatusPagamento.APROVADO;
    }

    public void recusar() {
        exigirPendente();
        status = StatusPagamento.RECUSADO;
    }

    public void estornar() {
        if (status == StatusPagamento.ESTORNADO) {
            return;
        }
        if (status != StatusPagamento.APROVADO) {
            throw new IllegalStateException("Somente pagamentos aprovados podem ser estornados.");
        }
        status = StatusPagamento.ESTORNADO;
    }

    private void exigirPendente() {
        if (status != StatusPagamento.PENDENTE) {
            throw new IllegalStateException("O pagamento não está pendente.");
        }
    }

    public PagamentoId getId() { return id; }
    public ReservaId getReservaId() { return reservaId; }
    public Dinheiro getValor() { return valor; }
    public StatusPagamento getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
}
