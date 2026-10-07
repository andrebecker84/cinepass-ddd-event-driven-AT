package br.com.cinepass.reserva.application;

import java.util.UUID;

/**
 * A Saga não concluiu a reserva e a compensou. Não é inconsistência: o estado final é
 * coerente nos três serviços (reserva cancelada, assentos liberados, pagamento estornado).
 */
public class ReservaCanceladaException extends RuntimeException {
    private final UUID reservaId;
    private final UUID pagamentoId;
    private final String motivo;
    private final boolean pagamentoEstornado;

    public ReservaCanceladaException(UUID reservaId, String motivo, UUID pagamentoId, boolean pagamentoEstornado) {
        super(pagamentoEstornado
                ? "A emissão do ingresso falhou. O pagamento foi estornado e a reserva cancelada."
                : "A emissão do ingresso falhou e o estorno não foi confirmado. A reserva foi cancelada e o estorno ficou pendente.");
        this.reservaId=reservaId; this.motivo=motivo; this.pagamentoId=pagamentoId; this.pagamentoEstornado=pagamentoEstornado;
    }

    public UUID getReservaId(){return reservaId;} public UUID getPagamentoId(){return pagamentoId;}
    public String getMotivo(){return motivo;} public boolean isPagamentoEstornado(){return pagamentoEstornado;}
}
