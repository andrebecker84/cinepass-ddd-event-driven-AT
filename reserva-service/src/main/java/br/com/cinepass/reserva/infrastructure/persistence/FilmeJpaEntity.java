package br.com.cinepass.reserva.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="filmes")
public class FilmeJpaEntity {
    @Id private UUID id;
    @Column(nullable=false) private String titulo;
    @Column(name="duracao_minutos", nullable=false) private int duracaoMinutos;
    @Column(nullable=false, length=10) private String classificacao;
    protected FilmeJpaEntity() {}
    public FilmeJpaEntity(UUID id, String titulo, int duracaoMinutos, String classificacao){this.id=id;this.titulo=titulo;this.duracaoMinutos=duracaoMinutos;this.classificacao=classificacao;}
    public UUID getId(){return id;} public String getTitulo(){return titulo;} public int getDuracaoMinutos(){return duracaoMinutos;} public String getClassificacao(){return classificacao;}
}
