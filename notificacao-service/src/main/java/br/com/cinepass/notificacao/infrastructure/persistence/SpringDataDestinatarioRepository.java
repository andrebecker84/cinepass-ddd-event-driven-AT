package br.com.cinepass.notificacao.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataDestinatarioRepository extends JpaRepository<DestinatarioJpaEntity, UUID> {
}
