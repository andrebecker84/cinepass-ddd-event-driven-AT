package br.com.cinepass.reserva.application;

import java.util.UUID;
public class ReservaNaoEncontradaException extends RuntimeException {
    public ReservaNaoEncontradaException(UUID id) { super("Reserva não encontrada: " + id); }
}
