package br.com.cinepass.reserva.domain.repository;

import br.com.cinepass.reserva.domain.model.Reserva;
import br.com.cinepass.reserva.domain.model.ReservaId;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository {
    Reserva salvar(Reserva reserva);
    Optional<Reserva> buscarPorId(ReservaId id);
    List<Reserva> listar();
}
