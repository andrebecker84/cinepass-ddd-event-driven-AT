package br.com.cinepass.ingresso.domain.repository;

import br.com.cinepass.ingresso.domain.model.Ingresso;
import br.com.cinepass.ingresso.domain.model.IngressoId;
import br.com.cinepass.ingresso.domain.model.ReservaId;

import java.util.Optional;

public interface IngressoRepository {
    Ingresso salvar(Ingresso ingresso);
    Optional<Ingresso> buscarPorId(IngressoId id);
    Optional<Ingresso> buscarPorReserva(ReservaId reservaId);
}
