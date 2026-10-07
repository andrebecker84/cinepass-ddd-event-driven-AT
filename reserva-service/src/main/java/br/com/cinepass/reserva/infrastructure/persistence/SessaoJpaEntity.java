package br.com.cinepass.reserva.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="sessoes")
public class SessaoJpaEntity {
    @Id private UUID id;
    @Column(name="filme_id", nullable=false) private UUID filmeId;
    @Column(nullable=false) private String sala;
    @Column(nullable=false) private LocalDateTime inicio;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal preco;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="sessao_assentos_disponiveis", joinColumns=@JoinColumn(name="sessao_id"))
    @Column(name="assento", nullable=false)
    private Set<String> assentosDisponiveis = new HashSet<>();
    @Version private long versao;

    protected SessaoJpaEntity() {}
    public SessaoJpaEntity(UUID id, UUID filmeId, String sala, LocalDateTime inicio, BigDecimal preco, Set<String> assentosDisponiveis){
        this.id=id;this.filmeId=filmeId;this.sala=sala;this.inicio=inicio;this.preco=preco;this.assentosDisponiveis=new HashSet<>(assentosDisponiveis);
    }
    public void atualizarAssentosDisponiveis(Set<String> assentos){this.assentosDisponiveis.clear();this.assentosDisponiveis.addAll(assentos);}
    public UUID getId(){return id;} public UUID getFilmeId(){return filmeId;} public String getSala(){return sala;} public LocalDateTime getInicio(){return inicio;}
    public BigDecimal getPreco(){return preco;} public Set<String> getAssentosDisponiveis(){return Set.copyOf(assentosDisponiveis);}
}
