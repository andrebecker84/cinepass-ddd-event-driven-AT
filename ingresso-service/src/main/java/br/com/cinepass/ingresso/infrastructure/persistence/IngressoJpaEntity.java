package br.com.cinepass.ingresso.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ingressos", indexes = @Index(name = "idx_ingresso_reserva", columnList = "reserva_id"))
public class IngressoJpaEntity {
    @Id private UUID id;
    @Column(name = "reserva_id", nullable = false) private UUID reservaId;
    @Column(name = "sessao_id", nullable = false) private UUID sessaoId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ingresso_assentos", joinColumns = @JoinColumn(name = "ingresso_id"))
    @Column(name = "assento", nullable = false)
    private List<String> assentos = new ArrayList<>();
    @Column(nullable = false, unique = true) private String codigo;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "emitido_em", nullable = false) private Instant emitidoEm;

    protected IngressoJpaEntity() {}
    public IngressoJpaEntity(UUID id, UUID reservaId, UUID sessaoId, List<String> assentos, String codigo, String status, Instant emitidoEm) {
        this.id=id; this.reservaId=reservaId; this.sessaoId=sessaoId; this.assentos=new ArrayList<>(assentos);
        this.codigo=codigo; this.status=status; this.emitidoEm=emitidoEm;
    }
    public UUID getId(){return id;} public UUID getReservaId(){return reservaId;} public UUID getSessaoId(){return sessaoId;}
    public List<String> getAssentos(){return List.copyOf(assentos);} public String getCodigo(){return codigo;}
    public String getStatus(){return status;} public Instant getEmitidoEm(){return emitidoEm;}
}
