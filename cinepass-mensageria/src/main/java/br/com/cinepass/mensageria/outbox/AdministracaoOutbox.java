package br.com.cinepass.mensageria.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Operações de suporte sobre o outbox. A principal é {@link #republicar}: devolve uma
 * mensagem já publicada à fila do relay, que a envia de novo, idêntica e com o mesmo
 * {@code eventId}. É a reprodução controlada do cenário real em que o relay cai entre a
 * confirmação do Kafka e a marcação da linha, e serve para demonstrar a idempotência
 * dos consumidores.
 */
public class AdministracaoOutbox {

    private static final Logger log = LoggerFactory.getLogger(AdministracaoOutbox.class);

    private final OutboxRepository repositorio;

    public AdministracaoOutbox(OutboxRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public boolean republicar(UUID eventId) {
        boolean encontrada = repositorio.marcarComoPendente(eventId) > 0;
        if (encontrada) {
            log.warn("Evento {} devolvido ao outbox para nova publicação (reentrega deliberada)", eventId);
        }
        return encontrada;
    }

    @Transactional(readOnly = true)
    public List<OutboxMensagem> daReserva(UUID reservaId) {
        return repositorio.findByReservaIdOrderByIdAsc(reservaId);
    }

    @Transactional(readOnly = true)
    public long pendentes() {
        return repositorio.countByPublicadoEmIsNull();
    }
}
