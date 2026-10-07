package br.com.cinepass.ingresso.application;

import java.util.List;
import java.util.UUID;

public record EmitirIngressoCommand(UUID reservaId, UUID sessaoId, List<String> assentos, boolean simularFalha) {}
