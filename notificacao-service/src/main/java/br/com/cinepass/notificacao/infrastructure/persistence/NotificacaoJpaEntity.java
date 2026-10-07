package br.com.cinepass.notificacao.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A restrição única em {@code event_id} é a segunda linha de defesa contra notificação
 * repetida. A primeira é a inbox: mesmo que ela falhasse, o banco recusaria a duplicata.
 */
@Entity
@Table(name = "notificacoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacao_evento", columnNames = "event_id"),
        indexes = @Index(name = "idx_notificacao_reserva", columnList = "reserva_id"))
public class NotificacaoJpaEntity {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "tipo_evento", nullable = false, length = 80) private String tipoEvento;
    @Column(name = "reserva_id", nullable = false) private UUID reservaId;
    @Column(name = "cliente_id", nullable = false) private UUID clienteId;
    @Column(nullable = false, length = 20) private String canal;
    @Column(nullable = false, length = 120) private String destinatario;
    @Column(nullable = false, length = 120) private String assunto;
    @Column(nullable = false, length = 500) private String mensagem;
    @Column(name = "correlation_id", length = 64) private String correlationId;
    @Column(name = "registrada_em", nullable = false) private Instant registradaEm;

    protected NotificacaoJpaEntity() {}

    public NotificacaoJpaEntity(UUID id, UUID eventId, String tipoEvento, UUID reservaId, UUID clienteId, String canal,
                                String destinatario, String assunto, String mensagem, String correlationId, Instant registradaEm) {
        this.id = id; this.eventId = eventId; this.tipoEvento = tipoEvento; this.reservaId = reservaId; this.clienteId = clienteId;
        this.canal = canal; this.destinatario = destinatario; this.assunto = assunto; this.mensagem = mensagem;
        this.correlationId = correlationId; this.registradaEm = registradaEm;
    }

    public UUID getId() { return id; } public UUID getEventId() { return eventId; } public String getTipoEvento() { return tipoEvento; }
    public UUID getReservaId() { return reservaId; } public UUID getClienteId() { return clienteId; } public String getCanal() { return canal; }
    public String getDestinatario() { return destinatario; } public String getAssunto() { return assunto; } public String getMensagem() { return mensagem; }
    public String getCorrelationId() { return correlationId; } public Instant getRegistradaEm() { return registradaEm; }
}
