package br.com.cinepass.reserva.domain.event;

import br.com.cinepass.reserva.domain.model.ClienteId;
import br.com.cinepass.reserva.domain.model.Dinheiro;
import br.com.cinepass.reserva.domain.model.ReservaId;

import java.time.Instant;
import java.util.UUID;

/** O pagamento da reserva foi aprovado; a emissão do ingresso é o próximo passo. */
public record PagamentoConfirmado(UUID eventId, ReservaId reservaId, ClienteId clienteId, long sequencia, Instant ocorridoEm,
                                  UUID pagamentoId, Dinheiro valor) implements EventoDeReserva {
}
