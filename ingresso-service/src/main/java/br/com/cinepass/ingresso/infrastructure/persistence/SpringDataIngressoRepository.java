package br.com.cinepass.ingresso.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface SpringDataIngressoRepository extends JpaRepository<IngressoJpaEntity, UUID> {
    Optional<IngressoJpaEntity> findFirstByReservaIdOrderByEmitidoEmDesc(UUID reservaId);
}
