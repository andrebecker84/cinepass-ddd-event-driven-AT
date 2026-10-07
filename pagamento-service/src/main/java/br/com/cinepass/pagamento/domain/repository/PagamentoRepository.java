package br.com.cinepass.pagamento.domain.repository;

import br.com.cinepass.pagamento.domain.model.Pagamento;
import br.com.cinepass.pagamento.domain.model.PagamentoId;
import br.com.cinepass.pagamento.domain.model.ReservaId;

import java.util.Optional;

public interface PagamentoRepository {
    Pagamento salvar(Pagamento pagamento);
    Optional<Pagamento> buscarPorId(PagamentoId id);
    Optional<Pagamento> buscarPorReservaId(ReservaId reservaId);
}
