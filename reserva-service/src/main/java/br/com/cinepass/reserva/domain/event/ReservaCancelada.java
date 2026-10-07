package br.com.cinepass.reserva.domain.event;

import br.com.cinepass.reserva.domain.model.ClienteId;
import br.com.cinepass.reserva.domain.model.Dinheiro;
import br.com.cinepass.reserva.domain.model.MotivoCancelamento;
import br.com.cinepass.reserva.domain.model.ReservaId;

import java.time.Instant;
import java.util.UUID;

/**
 * A reserva foi encerrada sem conclusão e os assentos voltaram para a sessão.
 * {@code pagamentoEstornado} informa se houve compensação de um pagamento já aprovado.
 */
public record ReservaCancelada(UUID eventId, ReservaId reservaId, ClienteId clienteId, long sequencia, Instant ocorridoEm,
                               MotivoCancelamento motivo, Dinheiro valorTotal, UUID pagamentoId,
                               boolean pagamentoEstornado) implements EventoDeReserva {
}
