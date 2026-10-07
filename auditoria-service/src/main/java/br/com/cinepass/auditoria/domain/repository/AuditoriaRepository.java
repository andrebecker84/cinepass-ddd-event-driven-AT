package br.com.cinepass.auditoria.domain.repository;

import br.com.cinepass.auditoria.domain.model.RegistroAuditoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriaRepository {

    void registrar(RegistroAuditoria registro);

    Optional<RegistroAuditoria> porEvento(UUID eventId);

    /** Filtros opcionais e combináveis. Sem nenhum, devolve os 100 mais recentes. */
    List<RegistroAuditoria> consultar(Filtro filtro);

    record Filtro(UUID reservaId, String correlationId, String eventType, UUID eventId) {
    }
}
