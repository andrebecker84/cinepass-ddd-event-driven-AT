package br.com.cinepass.reserva.infrastructure.persistence;

import br.com.cinepass.reserva.domain.model.Filme;
import br.com.cinepass.reserva.domain.model.FilmeId;
import br.com.cinepass.reserva.domain.repository.FilmeRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class FilmeRepositoryJpaAdapter implements FilmeRepository {
    private final SpringDataFilmeRepository repository;
    public FilmeRepositoryJpaAdapter(SpringDataFilmeRepository repository){this.repository=repository;}
    @Override public List<Filme> listar(){
        return repository.findAll().stream().map(e -> new Filme(new FilmeId(e.getId()), e.getTitulo(), e.getDuracaoMinutos(), e.getClassificacao())).toList();
    }
}
