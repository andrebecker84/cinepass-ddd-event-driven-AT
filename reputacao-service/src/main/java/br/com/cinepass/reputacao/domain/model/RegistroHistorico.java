package br.com.cinepass.reputacao.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Uma linha do histórico do cliente: o evento que a originou e o efeito que teve na reputação. */
public record RegistroHistorico(UUID eventId, UUID clienteId, UUID reservaId, String tipoEvento, String descricao,
                                long pontos, Instant ocorridoEm, String correlationId) {
}
