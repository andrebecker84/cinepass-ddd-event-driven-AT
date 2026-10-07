package br.com.cinepass.reserva.infrastructure.mensageria;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.correlacao.Correlacao;
import br.com.cinepass.mensageria.outbox.PublicadorOutbox;
import br.com.cinepass.reserva.application.port.PublicadorDeEventos;
import br.com.cinepass.reserva.domain.event.EventoDeReserva;
import br.com.cinepass.reserva.domain.event.PagamentoConfirmado;
import br.com.cinepass.reserva.domain.event.ReservaCancelada;
import br.com.cinepass.reserva.domain.event.ReservaConfirmada;
import br.com.cinepass.reserva.domain.event.ReservaCriada;
import br.com.cinepass.reserva.domain.model.AssentoId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Adaptador da porta {@link PublicadorDeEventos}: traduz cada evento de domínio para o
 * contrato publicado, envolve no envelope e entrega ao outbox.
 *
 * <p>Nada sai para o Kafka aqui. A linha do outbox é gravada na transação do passo da Saga,
 * e o relay a publica depois do commit.
 */
@Component
public class PublicadorDeEventosOutbox implements PublicadorDeEventos {

    static final int VERSAO_DO_CONTRATO = 1;

    private final PublicadorOutbox outbox;
    private final JsonMapper jsonMapper;
    private final String produtor;

    public PublicadorDeEventosOutbox(PublicadorOutbox outbox, JsonMapper jsonMapper,
                                     @Value("${spring.application.name}") String produtor) {
        this.outbox = outbox;
        this.jsonMapper = jsonMapper;
        this.produtor = produtor;
    }

    @Override
    public void publicar(List<EventoDeReserva> eventos) {
        String correlationId = Correlacao.atualOuNovo();
        for (EventoDeReserva evento : eventos) {
            outbox.publicar(Topicos.RESERVA_EVENTOS, envelope(evento, correlationId));
        }
    }

    EventoEnvelope envelope(EventoDeReserva evento, String correlationId) {
        return new EventoEnvelope(evento.eventId(), evento.tipo(), VERSAO_DO_CONTRATO, evento.reservaId().valor(),
                evento.sequencia(), evento.ocorridoEm(), correlationId, produtor, jsonMapper.valueToTree(payload(evento)));
    }

    private static Object payload(EventoDeReserva evento) {
        return switch (evento) {
            case ReservaCriada e -> new ContratosDeEvento.ReservaCriadaV1(e.clienteId().valor(), e.sessaoId().valor(),
                    assentos(e.assentos()), e.valorTotal().valor());
            case PagamentoConfirmado e -> new ContratosDeEvento.PagamentoConfirmadoV1(e.clienteId().valor(),
                    e.pagamentoId(), e.valor().valor());
            case ReservaConfirmada e -> new ContratosDeEvento.ReservaConfirmadaV1(e.clienteId().valor(), e.sessaoId().valor(),
                    assentos(e.assentos()), e.valorTotal().valor(), e.pagamentoId(), e.ingressoId());
            case ReservaCancelada e -> new ContratosDeEvento.ReservaCanceladaV1(e.clienteId().valor(), e.motivo().name(),
                    e.valorTotal().valor(), e.pagamentoId(), e.pagamentoEstornado());
        };
    }

    private static List<String> assentos(List<AssentoId> assentos) {
        return assentos.stream().map(AssentoId::valor).sorted().toList();
    }
}
