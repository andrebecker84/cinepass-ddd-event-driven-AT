package br.com.cinepass.auditoria.infrastructure.persistence;

import br.com.cinepass.auditoria.domain.model.RegistroAuditoria;
import br.com.cinepass.auditoria.domain.repository.AuditoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuditoriaRepositoryJpaAdapter implements AuditoriaRepository {

    private final SpringDataAuditoriaRepository repository;

    public AuditoriaRepositoryJpaAdapter(SpringDataAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(RegistroAuditoria r) {
        repository.save(new RegistroAuditoriaJpaEntity(r.eventId(), r.eventType(), r.eventVersion(), r.reservaId(), r.sequence(),
                r.occurredAt(), r.correlationId(), r.producer(), r.traceId(), r.topico(), r.particao(), r.offset(), r.payload(),
                r.registradoEm()));
    }

    @Override
    public Optional<RegistroAuditoria> porEvento(java.util.UUID eventId) {
        return repository.findById(eventId).map(AuditoriaRepositoryJpaAdapter::paraDominio);
    }

    /**
     * Com filtro por reserva, ordena pela sequência do evento: é a ordem em que os fatos
     * aconteceram. Sem ele, os mais recentes primeiro.
     */
    @Override
    public List<RegistroAuditoria> consultar(Filtro f) {
        Specification<RegistroAuditoriaJpaEntity> spec = Specification.unrestricted();
        if (f.reservaId() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("reservaId"), f.reservaId()));
        if (f.correlationId() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("correlationId"), f.correlationId()));
        if (f.eventType() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("eventType"), f.eventType()));
        if (f.eventId() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("eventId"), f.eventId()));

        Sort ordem = f.reservaId() != null
                ? Sort.by("sequence").ascending().and(Sort.by("registradoEm").ascending())
                : Sort.by("registradoEm").descending();
        return repository.findBy(spec, consulta -> consulta.sortBy(ordem).limit(100).all()).stream()
                .map(AuditoriaRepositoryJpaAdapter::paraDominio).toList();
    }

    private static RegistroAuditoria paraDominio(RegistroAuditoriaJpaEntity e) {
        return new RegistroAuditoria(e.getEventId(), e.getEventType(), e.getEventVersion(), e.getReservaId(), e.getSequence(),
                e.getOccurredAt(), e.getCorrelationId(), e.getProducer(), e.getTraceId(), e.getTopico(), e.getParticao(),
                e.getOffset(), e.getPayload(), e.getRegistradoEm());
    }
}
