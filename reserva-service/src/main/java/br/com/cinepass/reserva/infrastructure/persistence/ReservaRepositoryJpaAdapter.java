package br.com.cinepass.reserva.infrastructure.persistence;

import br.com.cinepass.reserva.domain.model.*;
import br.com.cinepass.reserva.domain.repository.ReservaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservaRepositoryJpaAdapter implements ReservaRepository {
    private final SpringDataReservaRepository repository;
    public ReservaRepositoryJpaAdapter(SpringDataReservaRepository repository){this.repository=repository;}

    @Override public Reserva salvar(Reserva r) {
        var e = new ReservaJpaEntity(r.getId().valor(), r.getClienteId().valor(), r.getSessaoId().valor(),
                r.getAssentos().stream().map(AssentoId::valor).toList(), r.getValorTotal().valor(), r.getStatus().name(),
                r.getPagamentoId(), r.getIngressoId(),
                r.getMotivoCancelamento() == null ? null : r.getMotivoCancelamento().name(), r.getSequenciaEventos(), r.getCriadaEm());
        return paraDominio(repository.save(e));
    }
    @Override public Optional<Reserva> buscarPorId(ReservaId id){return repository.findById(id.valor()).map(this::paraDominio);}
    @Override public List<Reserva> listar(){return repository.findAll().stream().map(this::paraDominio).toList();}
    private Reserva paraDominio(ReservaJpaEntity e){
        return Reserva.restaurar(new ReservaId(e.getId()), new ClienteId(e.getClienteId()), new SessaoId(e.getSessaoId()),
                e.getAssentos().stream().map(AssentoId::new).toList(), new Dinheiro(e.getValorTotal()), StatusReserva.valueOf(e.getStatus()),
                e.getPagamentoId(), e.getIngressoId(),
                e.getMotivoCancelamento() == null ? null : MotivoCancelamento.valueOf(e.getMotivoCancelamento()),
                e.getSequenciaEventos(), e.getCriadaEm());
    }
}
