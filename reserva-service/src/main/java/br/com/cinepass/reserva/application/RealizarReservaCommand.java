package br.com.cinepass.reserva.application;

import java.util.List;
import java.util.UUID;

public record RealizarReservaCommand(UUID clienteId, UUID sessaoId, List<String> assentos,
                                     boolean simularRecusaPagamento, boolean simularFalhaIngresso) {}
