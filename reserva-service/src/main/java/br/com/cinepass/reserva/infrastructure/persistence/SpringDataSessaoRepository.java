package br.com.cinepass.reserva.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataSessaoRepository extends JpaRepository<SessaoJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoJpaEntity s where s.id = :id")
    Optional<SessaoJpaEntity> bloquear(@Param("id") UUID id);
}
