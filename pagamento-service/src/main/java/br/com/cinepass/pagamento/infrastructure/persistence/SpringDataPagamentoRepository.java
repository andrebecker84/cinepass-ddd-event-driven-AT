package br.com.cinepass.pagamento.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface SpringDataPagamentoRepository extends JpaRepository<PagamentoJpaEntity, UUID> {
    Optional<PagamentoJpaEntity> findFirstByReservaIdOrderByCriadoEmDesc(UUID reservaId);
}
