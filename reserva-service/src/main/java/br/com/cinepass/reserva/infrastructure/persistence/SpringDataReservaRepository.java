package br.com.cinepass.reserva.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface SpringDataReservaRepository extends JpaRepository<ReservaJpaEntity, UUID> {}
