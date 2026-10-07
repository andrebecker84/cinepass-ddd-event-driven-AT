package br.com.cinepass.mensageria.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, UUID> {
}
