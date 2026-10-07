package br.com.cinepass.auditoria.application;

import br.com.cinepass.auditoria.domain.model.RegistroAuditoria;
import br.com.cinepass.auditoria.domain.repository.AuditoriaRepository;
import br.com.cinepass.mensageria.EventoEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditoriaApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaApplicationService.class);

    private final AuditoriaRepository repositorio;

    public AuditoriaApplicationService(AuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** De onde o evento veio no Kafka e em qual trace foi consumido. */
    public record Origem(String topico, int particao, long offset, String traceId) {
    }

    /** Todo evento é auditado, de qualquer tipo, inclusive os que nenhum outro serviço usa. */
    public void registrar(EventoEnvelope evento, Origem origem) {
        repositorio.registrar(new RegistroAuditoria(evento.eventId(), evento.eventType(), evento.eventVersion(),
                evento.reservaId(), evento.sequence(), evento.occurredAt(), evento.correlationId(), evento.producer(),
                origem.traceId(), origem.topico(), origem.particao(), origem.offset(), evento.payload().toString(),
                Instant.now()));
        log.info("Evento auditado: tipo={} sequence={} particao={} offset={} resultado=REGISTRADO",
                evento.eventType(), evento.sequence(), origem.particao(), origem.offset());
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultar(AuditoriaRepository.Filtro filtro) {
        return repositorio.consultar(filtro);
    }

    @Transactional(readOnly = true)
    public Optional<RegistroAuditoria> porEvento(UUID eventId) {
        return repositorio.porEvento(eventId);
    }
}
