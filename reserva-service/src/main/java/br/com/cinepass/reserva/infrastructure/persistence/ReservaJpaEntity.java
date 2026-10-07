package br.com.cinepass.reserva.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class ReservaJpaEntity {
    @Id private UUID id;
    @Column(name="cliente_id", nullable=false) private UUID clienteId;
    @Column(name="sessao_id", nullable=false) private UUID sessaoId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="reserva_assentos", joinColumns=@JoinColumn(name="reserva_id"))
    @Column(name="assento", nullable=false)
    private List<String> assentos = new ArrayList<>();
    @Column(name="valor_total", nullable=false, precision=12, scale=2) private BigDecimal valorTotal;
    @Column(nullable=false, length=40) private String status;
    @Column(name="pagamento_id") private UUID pagamentoId;
    @Column(name="ingresso_id") private UUID ingressoId;
    @Column(name="motivo_cancelamento", length=40) private String motivoCancelamento;
    @Column(name="sequencia_eventos", nullable=false) private long sequenciaEventos;
    @Column(name="criada_em", nullable=false) private Instant criadaEm;

    protected ReservaJpaEntity() {}
    public ReservaJpaEntity(UUID id, UUID clienteId, UUID sessaoId, List<String> assentos, BigDecimal valorTotal,
                            String status, UUID pagamentoId, UUID ingressoId, String motivoCancelamento, long sequenciaEventos,
                            Instant criadaEm) {
        this.id=id; this.clienteId=clienteId; this.sessaoId=sessaoId; this.assentos=new ArrayList<>(assentos);
        this.valorTotal=valorTotal; this.status=status; this.pagamentoId=pagamentoId; this.ingressoId=ingressoId;
        this.motivoCancelamento=motivoCancelamento; this.sequenciaEventos=sequenciaEventos; this.criadaEm=criadaEm;
    }
    public UUID getId(){return id;} public UUID getClienteId(){return clienteId;} public UUID getSessaoId(){return sessaoId;}
    public List<String> getAssentos(){return List.copyOf(assentos);} public BigDecimal getValorTotal(){return valorTotal;}
    public String getStatus(){return status;} public UUID getPagamentoId(){return pagamentoId;} public UUID getIngressoId(){return ingressoId;} public Instant getCriadaEm(){return criadaEm;}
    public String getMotivoCancelamento(){return motivoCancelamento;} public long getSequenciaEventos(){return sequenciaEventos;}
}
