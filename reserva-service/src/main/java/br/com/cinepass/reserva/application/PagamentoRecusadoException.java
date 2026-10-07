package br.com.cinepass.reserva.application;

import java.util.UUID;
public class PagamentoRecusadoException extends RuntimeException {
    private final UUID reservaId;
    public PagamentoRecusadoException(UUID reservaId) { super("Pagamento recusado para a reserva " + reservaId); this.reservaId=reservaId; }
    public UUID getReservaId(){return reservaId;}
}
