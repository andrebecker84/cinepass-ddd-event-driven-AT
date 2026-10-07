package br.com.cinepass.ingresso.application;

import java.util.UUID;

public class IngressoNaoEncontradoException extends RuntimeException {
    public IngressoNaoEncontradoException(UUID id) { super("Ingresso não encontrado para: " + id); }
}
