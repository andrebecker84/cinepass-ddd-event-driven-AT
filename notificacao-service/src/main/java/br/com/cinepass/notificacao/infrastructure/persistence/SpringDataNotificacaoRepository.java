package br.com.cinepass.notificacao.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataNotificacaoRepository extends JpaRepository<NotificacaoJpaEntity, UUID> {
    List<NotificacaoJpaEntity> findByReservaIdOrderByRegistradaEmAsc(UUID reservaId);
    List<NotificacaoJpaEntity> findTop50ByOrderByRegistradaEmDesc();
}
