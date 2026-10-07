package br.com.cinepass.reserva.application;

import br.com.cinepass.reserva.domain.model.AssentoId;
import br.com.cinepass.reserva.domain.repository.FilmeRepository;
import br.com.cinepass.reserva.domain.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CatalogoQueryService {
    private final FilmeRepository filmes;
    private final SessaoRepository sessoes;
    public CatalogoQueryService(FilmeRepository filmes, SessaoRepository sessoes){this.filmes=filmes;this.sessoes=sessoes;}

    @Transactional(readOnly=true)
    public List<FilmeView> filmes(){
        return filmes.listar().stream().map(f -> new FilmeView(f.id().valor(), f.titulo(), f.duracaoMinutos(), f.classificacao())).toList();
    }

    @Transactional(readOnly=true)
    public List<SessaoView> sessoes(){
        return sessoes.listar().stream().map(s -> new SessaoView(s.getId().valor(), s.getFilmeId().valor(), s.getSala(), s.getInicio(), s.getPreco().valor(),
                s.getAssentosDisponiveis().stream().map(AssentoId::valor).sorted().toList())).toList();
    }

    public record FilmeView(UUID id, String titulo, int duracaoMinutos, String classificacao) {}
    public record SessaoView(UUID id, UUID filmeId, String sala, LocalDateTime inicio, BigDecimal preco, List<String> assentosDisponiveis) {}
}
