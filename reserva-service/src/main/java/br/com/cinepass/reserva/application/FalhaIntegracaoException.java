package br.com.cinepass.reserva.application;

public class FalhaIntegracaoException extends RuntimeException {
    public FalhaIntegracaoException(String mensagem, Throwable cause) { super(mensagem, cause); }
}
