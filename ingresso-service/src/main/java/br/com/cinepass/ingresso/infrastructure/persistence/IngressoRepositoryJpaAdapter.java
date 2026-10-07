package br.com.cinepass.ingresso.infrastructure.persistence;

import br.com.cinepass.ingresso.domain.model.*;
import br.com.cinepass.ingresso.domain.repository.IngressoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class IngressoRepositoryJpaAdapter implements IngressoRepository {
    private final SpringDataIngressoRepository repository;
    public IngressoRepositoryJpaAdapter(SpringDataIngressoRepository repository) { this.repository = repository; }

    @Override public Ingresso salvar(Ingresso ingresso) {
        var entity = new IngressoJpaEntity(ingresso.getId().valor(), ingresso.getReservaId().valor(), ingresso.getSessaoId().valor(),
                ingresso.getAssentos(), ingresso.getCodigo().valor(), ingresso.getStatus().name(), ingresso.getEmitidoEm());
        return paraDominio(repository.save(entity));
    }
    @Override public Optional<Ingresso> buscarPorId(IngressoId id) { return repository.findById(id.valor()).map(this::paraDominio); }
    @Override public Optional<Ingresso> buscarPorReserva(ReservaId reservaId) { return repository.findFirstByReservaIdOrderByEmitidoEmDesc(reservaId.valor()).map(this::paraDominio); }
    private Ingresso paraDominio(IngressoJpaEntity e) {
        return Ingresso.restaurar(new IngressoId(e.getId()), new ReservaId(e.getReservaId()), new SessaoId(e.getSessaoId()),
                e.getAssentos(), new CodigoIngresso(e.getCodigo()), StatusIngresso.valueOf(e.getStatus()), e.getEmitidoEm());
    }
}
