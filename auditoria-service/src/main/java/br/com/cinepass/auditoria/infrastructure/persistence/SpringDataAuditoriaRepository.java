package br.com.cinepass.auditoria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface SpringDataAuditoriaRepository extends JpaRepository<RegistroAuditoriaJpaEntity, UUID>,
        JpaSpecificationExecutor<RegistroAuditoriaJpaEntity> {
}
