package br.com.cinepass.reserva;

import br.com.cinepass.mensageria.CabecalhosKafka;
import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.correlacao.Correlacao;
import br.com.cinepass.mensageria.outbox.OutboxRepository;
import br.com.cinepass.reserva.application.FalhaIntegracaoException;
import br.com.cinepass.reserva.application.port.IngressoGateway;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A Saga de ponta a ponta dentro do reserva-service, com PostgreSQL e Kafka reais.
 * Pagamento e ingresso são dublês no nível da porta: o que se verifica aqui é o que este
 * serviço grava e publica, não o comportamento dos outros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(InfraDeTeste.class)
class SagaDeReservaIntegracaoTest {

    private static final String SESSAO = "22222222-2222-2222-2222-222222222222";
    private static final String CLIENTE = "33333333-3333-3333-3333-333333333333";

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired OutboxRepository outbox;

    @MockitoBean PagamentoGateway pagamento;
    @MockitoBean IngressoGateway ingresso;

    @Test
    void caminhoFelizPublicaOsTresEventosEmOrdemNaMesmaParticaoComCorrelacaoETrace() throws Exception {
        UUID pagamentoId = UUID.randomUUID();
        UUID ingressoId = UUID.randomUUID();
        when(pagamento.cobrar(any(), any(), eq(false))).thenReturn(new PagamentoGateway.PagamentoResultado(pagamentoId, "APROVADO"));
        when(ingresso.emitir(any(), any(), any(), eq(false))).thenReturn(new IngressoGateway.IngressoResultado(ingressoId, "CINE-1", "EMITIDO"));

        MvcResult resultado = mvc.perform(post("/api/reservas").header(Correlacao.HEADER, "corr-teste-feliz-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("C1", false, false)))
                .andExpect(status().isCreated())
                .andExpect(header().string(Correlacao.HEADER, "corr-teste-feliz-001"))
                .andReturn();
        JsonNode reserva = json.readTree(resultado.getResponse().getContentAsString());
        String reservaId = reserva.get("reservaId").asString();
        assertEquals("CONFIRMADA", reserva.get("status").asString());

        List<ConsumerRecord<String, String>> registros =
                LeitorDoTopico.registrosDaChave(Topicos.RESERVA_EVENTOS, reservaId, 3, Duration.ofSeconds(20));

        assertEquals(List.of("ReservaCriada", "PagamentoConfirmado", "ReservaConfirmada"),
                registros.stream().map(r -> cabecalho(r, CabecalhosKafka.EVENT_TYPE)).toList(), "ordem de publicação");
        assertEquals(1, registros.stream().map(ConsumerRecord::partition).distinct().count(), "uma partição por reserva");
        assertTrue(registros.get(0).offset() < registros.get(1).offset() && registros.get(1).offset() < registros.get(2).offset());

        for (int i = 0; i < registros.size(); i++) {
            ConsumerRecord<String, String> registro = registros.get(i);
            JsonNode envelope = json.readTree(registro.value());
            assertEquals(reservaId, envelope.get("reservaId").asString());
            assertEquals(i + 1, envelope.get("sequence").asLong());
            assertEquals("corr-teste-feliz-001", envelope.get("correlationId").asString(), "correlação no envelope");
            assertEquals("corr-teste-feliz-001", cabecalho(registro, CabecalhosKafka.CORRELATION_ID), "correlação no cabeçalho");
            assertEquals(envelope.get("eventId").asString(), cabecalho(registro, CabecalhosKafka.EVENT_ID));
            assertNotNull(envelope.get("occurredAt"));
            assertEquals("reserva-service", envelope.get("producer").asString());
            assertEquals(CLIENTE, envelope.get("payload").get("clienteId").asString());
            assertNotNull(cabecalho(registro, "traceparent"), "o contexto de tracing atravessou o outbox");
        }

        String traceDaRequisicao = cabecalho(registros.getFirst(), "traceparent").split("-")[1];
        assertTrue(registros.stream().allMatch(r -> cabecalho(r, "traceparent").split("-")[1].equals(traceDaRequisicao)),
                "os três eventos pertencem ao trace da mesma requisição");
    }

    @Test
    void falhaNaEmissaoEstornaOPagamentoECancelaAReserva() throws Exception {
        UUID pagamentoId = UUID.randomUUID();
        when(pagamento.cobrar(any(), any(), anyBoolean())).thenReturn(new PagamentoGateway.PagamentoResultado(pagamentoId, "APROVADO"));
        when(ingresso.emitir(any(), any(), any(), eq(true))).thenThrow(new FalhaIntegracaoException("ingresso fora", null));
        when(pagamento.estornar(pagamentoId)).thenReturn(new PagamentoGateway.PagamentoResultado(pagamentoId, "ESTORNADO"));

        MvcResult resultado = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("C2", false, true)))
                .andExpect(status().isUnprocessableContent())
                .andReturn();
        JsonNode problema = json.readTree(resultado.getResponse().getContentAsString());
        String reservaId = problema.get("reservaId").asString();
        assertTrue(problema.get("pagamentoEstornado").asBoolean());
        verify(pagamento).estornar(pagamentoId);

        var registros = LeitorDoTopico.registrosDaChave(Topicos.RESERVA_EVENTOS, reservaId, 3, Duration.ofSeconds(20));
        assertEquals(List.of("ReservaCriada", "PagamentoConfirmado", "ReservaCancelada"),
                registros.stream().map(r -> cabecalho(r, CabecalhosKafka.EVENT_TYPE)).toList());
        JsonNode cancelada = json.readTree(registros.get(2).value()).get("payload");
        assertEquals("FALHA_EMISSAO_INGRESSO", cancelada.get("motivo").asString());
        assertTrue(cancelada.get("pagamentoEstornado").asBoolean());
    }

    @Test
    void pagamentoRecusadoCancelaSemTentarEmitirIngresso() throws Exception {
        when(pagamento.cobrar(any(), any(), eq(true))).thenReturn(new PagamentoGateway.PagamentoResultado(UUID.randomUUID(), "RECUSADO"));

        MvcResult resultado = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("C3", true, false)))
                .andExpect(status().isUnprocessableContent())
                .andReturn();
        String reservaId = json.readTree(resultado.getResponse().getContentAsString()).get("reservaId").asString();
        verifyNoInteractions(ingresso);

        var registros = LeitorDoTopico.registrosDaChave(Topicos.RESERVA_EVENTOS, reservaId, 2, Duration.ofSeconds(20));
        assertEquals(List.of("ReservaCriada", "ReservaCancelada"),
                registros.stream().map(r -> cabecalho(r, CabecalhosKafka.EVENT_TYPE)).toList());
        assertEquals(2, outbox.findByReservaIdOrderByIdAsc(UUID.fromString(reservaId)).size());
    }

    /**
     * Encontrado na execução real: seis reservas simultâneas na mesma sessão, em assentos
     * diferentes, disputam a mesma linha da sessão. Sem o bloqueio da linha, cinco falhavam
     * por conflito de versão. Com ele, todas são confirmadas.
     */
    @Test
    void reservasSimultaneasNaMesmaSessaoSaoTodasConfirmadas() throws Exception {
        when(pagamento.cobrar(any(), any(), eq(false)))
                .thenAnswer(i -> new PagamentoGateway.PagamentoResultado(UUID.randomUUID(), "APROVADO"));
        when(ingresso.emitir(any(), any(), any(), eq(false)))
                .thenAnswer(i -> new IngressoGateway.IngressoResultado(UUID.randomUUID(), "CINE-X", "EMITIDO"));

        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var respostas = java.util.stream.IntStream.rangeClosed(3, 10)
                    .mapToObj(n -> executor.submit(() -> mvc.perform(post("/api/reservas")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo("E" + n, false, false)))
                            .andReturn().getResponse().getStatus()))
                    .toList();
            for (var resposta : respostas) {
                assertEquals(201, resposta.get());
            }
        }
    }

    private static String corpo(String assento, boolean recusarPagamento, boolean falharIngresso) {
        return """
                {"clienteId":"%s","sessaoId":"%s","assentos":["%s"],"simularRecusaPagamento":%s,"simularFalhaIngresso":%s}
                """.formatted(CLIENTE, SESSAO, assento, recusarPagamento, falharIngresso);
    }

    private static String cabecalho(ConsumerRecord<String, String> registro, String nome) {
        var h = registro.headers().lastHeader(nome);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}
