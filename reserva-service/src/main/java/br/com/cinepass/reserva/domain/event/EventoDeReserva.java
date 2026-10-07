package br.com.cinepass.reserva.domain.event;

import br.com.cinepass.reserva.domain.model.ClienteId;
import br.com.cinepass.reserva.domain.model.ReservaId;

import java.time.Instant;
import java.util.UUID;

/**
 * Fatos relevantes do ciclo de vida de uma {@code Reserva}, registrados pelo próprio
 * agregado no momento da transição de estado.
 *
 * <p>O {@code eventId} nasce aqui, e não na publicação: é o mesmo identificador que viaja
 * no Kafka e que os consumidores usam para reconhecer uma reentrega. A {@code sequencia}
 * numera os eventos da reserva (1, 2, 3...) e torna verificável a ordem de chegada.
 *
 * <p>São tipos de domínio, sem anotação de framework. A tradução para o contrato publicado
 * fica na infraestrutura, em {@code PublicadorDeEventosOutbox}.
 */
public sealed interface EventoDeReserva
        permits ReservaCriada, PagamentoConfirmado, ReservaConfirmada, ReservaCancelada {

    UUID eventId();

    ReservaId reservaId();

    ClienteId clienteId();

    long sequencia();

    Instant ocorridoEm();

    default String tipo() {
        return getClass().getSimpleName();
    }
}
