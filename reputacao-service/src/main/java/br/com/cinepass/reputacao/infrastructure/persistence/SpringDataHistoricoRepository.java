package br.com.cinepass.reputacao.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataHistoricoRepository extends JpaRepository<HistoricoJpaEntity, Long> {
    List<HistoricoJpaEntity> findByClienteIdOrderByOcorridoEmAsc(UUID clienteId);
}
