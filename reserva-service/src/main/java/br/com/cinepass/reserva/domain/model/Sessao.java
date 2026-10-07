package br.com.cinepass.reserva.domain.model;

import java.time.LocalDateTime;
import java.util.*;

public class Sessao {
    private final SessaoId id;
    private final FilmeId filmeId;
    private final String sala;
    private final LocalDateTime inicio;
    private final Dinheiro preco;
    private final Set<AssentoId> assentosDisponiveis;

    public Sessao(SessaoId id, FilmeId filmeId, String sala, LocalDateTime inicio, Dinheiro preco, Set<AssentoId> assentosDisponiveis) {
        this.id = Objects.requireNonNull(id); this.filmeId = Objects.requireNonNull(filmeId);
        this.sala = Objects.requireNonNull(sala); this.inicio = Objects.requireNonNull(inicio); this.preco = Objects.requireNonNull(preco);
        this.assentosDisponiveis = new HashSet<>(Objects.requireNonNull(assentosDisponiveis));
    }

    public void reservar(List<AssentoId> assentos) {
        if (assentos == null || assentos.isEmpty()) throw new IllegalArgumentException("Selecione ao menos um assento.");
        if (new HashSet<>(assentos).size() != assentos.size()) throw new IllegalArgumentException("Há assentos duplicados na solicitação.");
        var indisponiveis = assentos.stream().filter(a -> !assentosDisponiveis.contains(a)).toList();
        if (!indisponiveis.isEmpty()) throw new AssentoIndisponivelException(indisponiveis);
        assentosDisponiveis.removeAll(assentos);
    }

    public void liberar(List<AssentoId> assentos) { assentosDisponiveis.addAll(assentos); }

    public SessaoId getId(){return id;} public FilmeId getFilmeId(){return filmeId;} public String getSala(){return sala;}
    public LocalDateTime getInicio(){return inicio;} public Dinheiro getPreco(){return preco;}
    public Set<AssentoId> getAssentosDisponiveis(){return Set.copyOf(assentosDisponiveis);}
}
