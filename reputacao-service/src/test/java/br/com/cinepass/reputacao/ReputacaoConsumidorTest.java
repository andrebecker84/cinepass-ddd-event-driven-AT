package br.com.cinepass.reputacao;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.inbox.EventoProcessadoRepository;
import br.com.cinepass.reputacao.application.ReputacaoApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "cinepass.mensageria.consumidor.espera-inicial-ms=200")
@ActiveProfiles("test")
@Import(InfraDeTeste.class)
class ReputacaoConsumidorTest {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired JsonMapper json;
    @Autowired ReputacaoApplicationService reputacao;
    @Autowired EventoProcessadoRepository inbox;
    @Autowired KafkaListenerEndpointRegistry ouvintes;

    @BeforeEach
    void consumidorPronto() {
        EventosDeTeste.aguardarParticoesAtribuidas(ouvintes);
    }

    @Test
    void reprocessarOMesmoEventoNaoIncrementaDeNovo() {
        UUID cliente = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        var evento = EventosDeTeste.confirmada(json, reservaId, 3, cliente, "79.80");

        EventosDeTeste.publicar(kafka, json, evento);
        EventosDeTeste.publicar(kafka, json, evento);
        EventosDeTeste.publicar(kafka, json, evento);
        var marcador = EventosDeTeste.evento(json, "ReservaCriada", reservaId, 4, cliente, Map.of());
        EventosDeTeste.publicar(kafka, json, marcador);
        await().atMost(Duration.ofSeconds(20)).until(() -> inbox.existsById(marcador.eventId()));

        var r = reputacao.reputacao(cliente).orElseThrow();
        assertEquals(1, r.getReservasConfirmadas(), "três entregas do mesmo evento contam uma vez");
        assertEquals(79, r.getPontos());
        assertEquals(0, new BigDecimal("79.80").compareTo(r.getValorTotalGasto()));
        assertEquals(1, reputacao.historico(cliente).size(), "uma linha de histórico por evento");
    }

    /**
     * A chave de partição é a reserva, não o cliente. Nove reservas do mesmo cliente, espalhadas
     * pelas três partições, são processadas ao mesmo tempo por três threads, e todas atualizam a
     * mesma linha. Sem o bloqueio da linha, uma atualização apagaria a outra.
     */
    @Test
    void reservasDoMesmoClienteEmParticoesDiferentesNaoPerdemAtualizacao() {
        UUID cliente = UUID.randomUUID();
        List<EventoEnvelope> eventos = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            eventos.add(EventosDeTeste.confirmada(json, EventosDeTeste.reservaNaParticao(i % 3), 3, cliente, "10.00"));
        }
        eventos.forEach(e -> EventosDeTeste.publicar(kafka, json, e));

        await().atMost(Duration.ofSeconds(30)).until(() -> eventos.stream().allMatch(e -> inbox.existsById(e.eventId())));
        var r = reputacao.reputacao(cliente).orElseThrow();
        assertEquals(9, r.getReservasConfirmadas());
        assertEquals(90, r.getPontos());
    }

    @Test
    void recusaDePagamentoPenalizaEFalhaDaPlataformaNao() {
        UUID cliente = UUID.randomUUID();
        var recusa = EventosDeTeste.evento(json, "ReservaCancelada", UUID.randomUUID(), 2, cliente,
                Map.of("motivo", "PAGAMENTO_RECUSADO", "valorTotal", new BigDecimal("39.90")));
        var falha = EventosDeTeste.evento(json, "ReservaCancelada", UUID.randomUUID(), 3, cliente,
                Map.of("motivo", "FALHA_EMISSAO_INGRESSO", "valorTotal", new BigDecimal("39.90"), "pagamentoEstornado", true));
        EventosDeTeste.publicar(kafka, json, recusa);
        EventosDeTeste.publicar(kafka, json, falha);

        await().atMost(Duration.ofSeconds(20)).until(() -> inbox.existsById(recusa.eventId()) && inbox.existsById(falha.eventId()));
        var r = reputacao.reputacao(cliente).orElseThrow();
        assertEquals(2, r.getReservasCanceladas());
        assertEquals(1, r.getPagamentosRecusados());
    }
}
