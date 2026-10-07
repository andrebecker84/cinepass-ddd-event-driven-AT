package br.com.cinepass.reserva.domain.event;

import br.com.cinepass.reserva.domain.model.AssentoId;
import br.com.cinepass.reserva.domain.model.ClienteId;
import br.com.cinepass.reserva.domain.model.Dinheiro;
import br.com.cinepass.reserva.domain.model.ReservaId;
import br.com.cinepass.reserva.domain.model.SessaoId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pagamento aprovado e ingresso emitido: a reserva está concluída. */
public record ReservaConfirmada(UUID eventId, ReservaId reservaId, ClienteId clienteId, long sequencia, Instant ocorridoEm,
                                SessaoId sessaoId, List<AssentoId> assentos, Dinheiro valorTotal,
                                UUID pagamentoId, UUID ingressoId) implements EventoDeReserva {

    public ReservaConfirmada {
        assentos = List.copyOf(assentos);
    }
}
