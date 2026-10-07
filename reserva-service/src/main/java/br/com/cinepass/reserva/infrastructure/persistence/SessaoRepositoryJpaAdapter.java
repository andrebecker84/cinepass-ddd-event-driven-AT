package br.com.cinepass.reserva.infrastructure.persistence;

import br.com.cinepass.reserva.application.SessaoNaoEncontradaException;
import br.com.cinepass.reserva.domain.model.*;
import br.com.cinepass.reserva.domain.repository.SessaoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class SessaoRepositoryJpaAdapter implements SessaoRepository {
    private final SpringDataSessaoRepository repository;
    public SessaoRepositoryJpaAdapter(SpringDataSessaoRepository repository){this.repository=repository;}
    @Override public Optional<Sessao> buscarPorId(SessaoId id){return repository.findById(id.valor()).map(this::paraDominio);}
    @Override public Optional<Sessao> buscarPorIdParaAtualizar(SessaoId id){return repository.bloquear(id.valor()).map(this::paraDominio);}
    @Override public Sessao salvar(Sessao sessao){
        SessaoJpaEntity entity = repository.findById(sessao.getId().valor())
                .orElseThrow(() -> new SessaoNaoEncontradaException(sessao.getId().valor()));
        entity.atualizarAssentosDisponiveis(sessao.getAssentosDisponiveis().stream().map(AssentoId::valor).collect(java.util.stream.Collectors.toSet()));
        return paraDominio(repository.save(entity));
    }
    @Override public List<Sessao> listar(){return repository.findAll().stream().map(this::paraDominio).toList();}
    private Sessao paraDominio(SessaoJpaEntity e){
        var assentos=e.getAssentosDisponiveis().stream().map(AssentoId::new).collect(java.util.stream.Collectors.toSet());
        return new Sessao(new SessaoId(e.getId()), new FilmeId(e.getFilmeId()), e.getSala(), e.getInicio(), new Dinheiro(e.getPreco()), assentos);
    }
}
