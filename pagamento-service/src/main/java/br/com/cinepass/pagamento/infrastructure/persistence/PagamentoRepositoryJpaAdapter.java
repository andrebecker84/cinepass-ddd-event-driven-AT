package br.com.cinepass.pagamento.infrastructure.persistence;

import br.com.cinepass.pagamento.domain.model.*;
import br.com.cinepass.pagamento.domain.repository.PagamentoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PagamentoRepositoryJpaAdapter implements PagamentoRepository {
    private final SpringDataPagamentoRepository repository;

    public PagamentoRepositoryJpaAdapter(SpringDataPagamentoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pagamento salvar(Pagamento pagamento) {
        var entity = new PagamentoJpaEntity(
                pagamento.getId().valor(), pagamento.getReservaId().valor(), pagamento.getValor().valor(),
                pagamento.getStatus().name(), pagamento.getCriadoEm());
        return paraDominio(repository.save(entity));
    }

    @Override
    public Optional<Pagamento> buscarPorId(PagamentoId id) {
        return repository.findById(id.valor()).map(this::paraDominio);
    }

    @Override
    public Optional<Pagamento> buscarPorReservaId(ReservaId reservaId) {
        return repository.findFirstByReservaIdOrderByCriadoEmDesc(reservaId.valor()).map(this::paraDominio);
    }

    private Pagamento paraDominio(PagamentoJpaEntity e) {
        return Pagamento.restaurar(
                new PagamentoId(e.getId()), new ReservaId(e.getReservaId()), new Dinheiro(e.getValor()),
                StatusPagamento.valueOf(e.getStatus()), e.getCriadoEm());
    }
}
