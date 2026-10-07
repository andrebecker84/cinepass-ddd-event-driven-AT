package br.com.cinepass.mensageria.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha do outbox: o evento já serializado, gravado na mesma transação que alterou
 * o agregado. O relay a lê depois e a publica no Kafka.
 *
 * <p>O {@code id} sequencial define a ordem de publicação. É ele, e não o relógio, que
 * garante que {@code ReservaCriada} saia antes de {@code ReservaConfirmada}.
 *
 * <p>{@code cabecalhosTracing} guarda o contexto de rastreamento da requisição que gerou o
 * evento ({@code traceparent} W3C). Sem ele, a publicação feita mais tarde pelo relay
 * começaria um trace novo, e o Zipkin mostraria a operação partida em dois.
 */
@Entity
@Table(name = "outbox_mensagens", indexes = @Index(name = "idx_outbox_pendentes", columnList = "publicado_em, id"))
public class OutboxMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String topico;

    @Column(nullable = false, length = 80)
    private String chave;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "cabecalhos_tracing", columnDefinition = "text")
    private String cabecalhosTracing;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    protected OutboxMensagem() {
    }

    OutboxMensagem(UUID eventId, String eventType, String topico, String chave, UUID reservaId,
                   String correlationId, String payload, String cabecalhosTracing, Instant criadoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.topico = topico;
        this.chave = chave;
        this.reservaId = reservaId;
        this.correlationId = correlationId;
        this.payload = payload;
        this.cabecalhosTracing = cabecalhosTracing;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopico() { return topico; }
    public String getChave() { return chave; }
    public UUID getReservaId() { return reservaId; }
    public String getCorrelationId() { return correlationId; }
    public String getPayload() { return payload; }
    public String getCabecalhosTracing() { return cabecalhosTracing; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getPublicadoEm() { return publicadoEm; }
    public int getTentativas() { return tentativas; }
    public String getUltimoErro() { return ultimoErro; }
}
