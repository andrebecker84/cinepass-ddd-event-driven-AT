package br.com.cinepass.mensageria.inbox;

import br.com.cinepass.mensageria.EventoEnvelope;

/**
 * A regra de negócio do consumidor para um evento. Roda dentro da mesma transação que
 * registra o evento na inbox: o efeito e o registro de que ele aconteceu commitam juntos.
 */
@FunctionalInterface
public interface ManipuladorDeEvento {

    void tratar(EventoEnvelope evento);
}
