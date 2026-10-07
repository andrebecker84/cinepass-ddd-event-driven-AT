package br.com.cinepass.mensageria.outbox;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.observabilidade.ContextoDeRastreamento;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Publicação transacional (Transactional Outbox): o evento vira uma linha na mesma
 * transação que alterou o agregado. Ou os dois são gravados, ou nenhum.
 *
 * <p>{@link Propagation#MANDATORY}: chamar fora de transação é erro de programação e
 * falha alto. Sem transação em curso, a gravação seria independente da mudança de estado,
 * que é exatamente a gravação dupla que o padrão existe para evitar.
 */
public class PublicadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final OutboxRepository repositorio;
    private final JsonMapper jsonMapper;
    private final ContextoDeRastreamento rastreamento;

    public PublicadorOutbox(OutboxRepository repositorio, JsonMapper jsonMapper, ContextoDeRastreamento rastreamento) {
        this.repositorio = repositorio;
        this.jsonMapper = jsonMapper;
        this.rastreamento = rastreamento;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(String topico, EventoEnvelope envelope) {
        String payload = jsonMapper.writeValueAsString(envelope);
        String cabecalhos = jsonMapper.writeValueAsString(rastreamento.capturar());
        repositorio.save(new OutboxMensagem(envelope.eventId(), envelope.eventType(), topico, envelope.chave(),
                envelope.reservaId(), envelope.correlationId(), payload, cabecalhos, Instant.now()));
        try (var mdc = MdcDoEvento.de(envelope)) {
            log.info("Evento registrado no outbox: tipo={} sequence={} topico={}",
                    envelope.eventType(), envelope.sequence(), topico);
        }
    }
}
