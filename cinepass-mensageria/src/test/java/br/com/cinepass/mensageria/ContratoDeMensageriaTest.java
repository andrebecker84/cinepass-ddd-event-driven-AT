package br.com.cinepass.mensageria;

import br.com.cinepass.mensageria.correlacao.Correlacao;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContratoDeMensageriaTest {

    private static EventoEnvelope envelope(UUID reservaId) {
        return new EventoEnvelope(UUID.randomUUID(), "ReservaCriada", 1, reservaId, 1, Instant.parse("2026-10-02T12:00:00Z"),
                "corr-0001-teste", "reserva-service", JsonNodeFactory.instance.objectNode().put("clienteId", "c1"));
    }

    @Test
    void aChaveDePublicacaoEhOIdentificadorDaReserva() {
        UUID reservaId = UUID.randomUUID();
        assertEquals(reservaId.toString(), envelope(reservaId).chave());
    }

    @Test
    void envelopeSemCampoObrigatorioNaoExiste() {
        assertThrows(NullPointerException.class, () -> new EventoEnvelope(UUID.randomUUID(), "X", 1, null, 1, Instant.now(),
                "corr-0001-teste", "p", JsonNodeFactory.instance.objectNode()));
        assertThrows(NullPointerException.class, () -> new EventoEnvelope(UUID.randomUUID(), "X", 1, UUID.randomUUID(), 1,
                Instant.now(), null, "p", JsonNodeFactory.instance.objectNode()));
        assertThrows(IllegalArgumentException.class, () -> new EventoEnvelope(UUID.randomUUID(), "X", 1, UUID.randomUUID(), 0,
                Instant.now(), "corr-0001-teste", "p", JsonNodeFactory.instance.objectNode()));
    }

    @Test
    void oEnvelopeSobreviveAIdaEVoltaEmJson() {
        JsonMapper json = JsonMapper.builder().build();
        EventoEnvelope original = envelope(UUID.randomUUID());
        String texto = json.writeValueAsString(original);
        assertTrue(texto.contains("\"occurredAt\":\"2026-10-02T12:00:00Z\""), "data em ISO-8601: " + texto);
        assertEquals(original, json.readValue(texto, EventoEnvelope.class));
    }

    @Test
    void cadaConsumidorTemSuaPropriaDlt() {
        assertEquals("cinepass.reserva.eventos.notificacao.DLT", Topicos.dltDe(Topicos.RESERVA_EVENTOS, "notificacao"));
        assertNotEquals(Topicos.dltDe(Topicos.RESERVA_EVENTOS, "auditoria"), Topicos.dltDe(Topicos.RESERVA_EVENTOS, "reputacao"));
    }

    @Test
    void correlationIdRecebidoSoPassaComFormatoSeguro() {
        assertEquals("corr-0001-teste", Correlacao.validarOuGerar("corr-0001-teste"));
        String gerado = Correlacao.validarOuGerar("abc\r\nlinha falsa no log");
        assertDoesNotThrow(() -> UUID.fromString(gerado), "valor perigoso é substituído por um UUID novo");
        assertDoesNotThrow(() -> UUID.fromString(Correlacao.validarOuGerar(null)));
    }

    @Test
    void oMdcDoEventoDevolveOContextoAnteriorAoFechar() {
        MDC.put(Correlacao.MDC_CORRELATION_ID, "da-requisicao-123");
        try (var mdc = MdcDoEvento.de(envelope(UUID.randomUUID()))) {
            assertEquals("corr-0001-teste", MDC.get(Correlacao.MDC_CORRELATION_ID));
            assertNotNull(MDC.get(Correlacao.MDC_EVENT_ID));
        }
        assertEquals("da-requisicao-123", MDC.get(Correlacao.MDC_CORRELATION_ID));
        assertNull(MDC.get(Correlacao.MDC_EVENT_ID));
        MDC.clear();
    }
}
