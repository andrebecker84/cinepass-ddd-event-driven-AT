package br.com.cinepass.reserva.application.port;

import br.com.cinepass.reserva.application.RealizarReservaCommand;
import br.com.cinepass.reserva.application.ReservaDetalhe;

public interface ReservaOrquestrador {
    ReservaDetalhe realizar(RealizarReservaCommand command);
}
