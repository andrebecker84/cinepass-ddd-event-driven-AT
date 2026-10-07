package br.com.cinepass.reputacao.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reputação de um cliente na plataforma: o equivalente, no CinePass, à reputação do
 * freelancer no enunciado original. É construída só a partir de eventos; nenhum outro
 * serviço escreve aqui.
 *
 * <p>Regras:
 * <ul>
 *   <li>Reserva confirmada: +1 confirmada, +1 ponto por real gasto, soma ao valor total.</li>
 *   <li>Pagamento recusado: +1 recusa. É comportamento do cliente e pesa na reputação.</li>
 *   <li>Falha da plataforma (ingresso não emitido, pagamento fora do ar): registra no
 *       histórico, sem penalidade. O cliente não teve culpa.</li>
 * </ul>
 *
 * <p>Os contadores são incrementais, e é por isso que a idempotência importa aqui mais que
 * em qualquer outro consumidor: reaplicar o mesmo evento daria pontos em dobro.
 */
public class ReputacaoCliente {

    private final UUID clienteId;
    private int reservasConfirmadas;
    private int reservasCanceladas;
    private int pagamentosRecusados;
    private long pontos;
    private BigDecimal valorTotalGasto;
    private Instant atualizadaEm;

    private ReputacaoCliente(UUID clienteId, int reservasConfirmadas, int reservasCanceladas, int pagamentosRecusados,
                             long pontos, BigDecimal valorTotalGasto, Instant atualizadaEm) {
        this.clienteId = Objects.requireNonNull(clienteId);
        this.reservasConfirmadas = reservasConfirmadas;
        this.reservasCanceladas = reservasCanceladas;
        this.pagamentosRecusados = pagamentosRecusados;
        this.pontos = pontos;
        this.valorTotalGasto = Objects.requireNonNull(valorTotalGasto);
        this.atualizadaEm = atualizadaEm;
    }

    public static ReputacaoCliente restaurar(UUID clienteId, int confirmadas, int canceladas, int recusados, long pontos,
                                             BigDecimal valorTotalGasto, Instant atualizadaEm) {
        return new ReputacaoCliente(clienteId, confirmadas, canceladas, recusados, pontos, valorTotalGasto, atualizadaEm);
    }

    /** @return pontos ganhos */
    public long registrarReservaConfirmada(BigDecimal valor) {
        long ganhos = valor.setScale(0, RoundingMode.DOWN).longValueExact();
        reservasConfirmadas++;
        pontos += ganhos;
        valorTotalGasto = valorTotalGasto.add(valor).setScale(2, RoundingMode.HALF_UP);
        atualizadaEm = Instant.now();
        return ganhos;
    }

    /** @return true se o cancelamento pesou na reputação do cliente */
    public boolean registrarCancelamento(String motivo) {
        reservasCanceladas++;
        atualizadaEm = Instant.now();
        if ("PAGAMENTO_RECUSADO".equals(motivo)) {
            pagamentosRecusados++;
            return true;
        }
        return false;
    }

    public Nivel nivel() {
        if (pontos >= 500) return Nivel.OURO;
        if (pontos >= 100) return Nivel.PRATA;
        return Nivel.BRONZE;
    }

    public enum Nivel { BRONZE, PRATA, OURO }

    public UUID getClienteId() { return clienteId; }
    public int getReservasConfirmadas() { return reservasConfirmadas; }
    public int getReservasCanceladas() { return reservasCanceladas; }
    public int getPagamentosRecusados() { return pagamentosRecusados; }
    public long getPontos() { return pontos; }
    public BigDecimal getValorTotalGasto() { return valorTotalGasto; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
