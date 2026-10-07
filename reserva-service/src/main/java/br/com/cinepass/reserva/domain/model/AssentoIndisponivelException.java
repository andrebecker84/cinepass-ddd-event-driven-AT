package br.com.cinepass.reserva.domain.model;

import java.util.List;

public class AssentoIndisponivelException extends RuntimeException {
    public AssentoIndisponivelException(List<AssentoId> assentos) {
        super("Assento(s) indisponível(is): " + assentos.stream().map(AssentoId::valor).toList());
    }
}
