package br.com.cinepass.reserva.domain.event;

import br.com.cinepass.reserva.domain.model.AssentoId;
import br.com.cinepass.reserva.domain.model.ClienteId;
import br.com.cinepass.reserva.domain.model.Dinheiro;
import br.com.cinepass.reserva.domain.model.ReservaId;
import br.com.cinepass.reserva.domain.model.SessaoId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Os assentos foram bloqueados na sessão e a reserva passou a existir. */
public record ReservaCriada(UUID eventId, ReservaId reservaId, ClienteId clienteId, long sequencia, Instant ocorridoEm,
                            SessaoId sessaoId, List<AssentoId> assentos, Dinheiro valorTotal) implements EventoDeReserva {

    public ReservaCriada {
        assentos = List.copyOf(assentos);
    }
}
