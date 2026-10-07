package br.com.cinepass.reserva.application.port;

import br.com.cinepass.reserva.domain.event.EventoDeReserva;

import java.util.List;

/**
 * Porta de saída para os eventos de domínio. O serviço de aplicação a chama dentro da
 * transação que grava o agregado; a implementação decide como o evento sai do processo.
 * Aqui ela é o outbox, e por isso precisa da transação em curso.
 */
public interface PublicadorDeEventos {

    void publicar(List<EventoDeReserva> eventos);
}
