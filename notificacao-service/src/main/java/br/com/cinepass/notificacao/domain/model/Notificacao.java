package br.com.cinepass.notificacao.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Notificação registrada em resposta a um evento da reserva. O {@code eventId} de origem é
 * único na tabela: um mesmo evento nunca gera duas notificações, mesmo que chegue duas vezes.
 *
 * <p>O destinatário é guardado já mascarado. O envio real (SMTP, push) está fora do escopo;
 * registrar a notificação é o efeito observável do serviço.
 */
public record Notificacao(UUID id, UUID eventId, String tipoEvento, UUID reservaId, UUID clienteId, String canal,
                          String destinatario, String assunto, String mensagem, String correlationId, Instant registradaEm) {
    public Notificacao {
        Objects.requireNonNull(id); Objects.requireNonNull(eventId); Objects.requireNonNull(tipoEvento);
        Objects.requireNonNull(reservaId); Objects.requireNonNull(clienteId); Objects.requireNonNull(canal);
        Objects.requireNonNull(destinatario); Objects.requireNonNull(assunto); Objects.requireNonNull(mensagem);
        Objects.requireNonNull(registradaEm);
    }

    public static Notificacao porEmail(UUID eventId, String tipoEvento, UUID reservaId, Destinatario destinatario,
                                       ModeloDeMensagem.Conteudo conteudo, String correlationId) {
        return new Notificacao(UUID.randomUUID(), eventId, tipoEvento, reservaId, destinatario.clienteId(), "EMAIL",
                destinatario.email().mascarado(), conteudo.assunto(), conteudo.mensagem(), correlationId, Instant.now());
    }
}
