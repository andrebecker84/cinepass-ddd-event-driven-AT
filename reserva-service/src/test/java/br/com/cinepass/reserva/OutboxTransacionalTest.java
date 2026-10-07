package br.com.cinepass.reserva;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.outbox.OutboxRepository;
import br.com.cinepass.mensageria.outbox.PublicadorOutbox;
import br.com.cinepass.reserva.application.RealizarReservaCommand;
import br.com.cinepass.reserva.application.ReservaApplicationService;
import br.com.cinepass.reserva.application.port.IngressoGateway;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Os dois lados da publicação transacional: o evento só existe se a mudança de estado
 * existir, e o que foi commitado acaba publicado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InfraDeTeste.class)
class OutboxTransacionalTest {

    private static final UUID SESSAO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLIENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired ReservaApplicationService reservas;
    @Autowired OutboxRepository outbox;
    @Autowired PublicadorOutbox publicador;
    @Autowired TransactionTemplate transacao;

    @MockitoBean PagamentoGateway pagamento;
    @MockitoBean IngressoGateway ingresso;

    @Test
    void rollbackDaMudancaDeEstadoLevaJuntoOEvento() {
        AtomicReference<UUID> reservaId = new AtomicReference<>();
        assertThrows(IllegalStateException.class, () -> transacao.executeWithoutResult(status -> {
            reservaId.set(reservas.iniciar(new RealizarReservaCommand(CLIENTE, SESSAO, List.of("D1"), false, false)).reservaId());
            throw new IllegalStateException("falha depois de gravar a reserva e o evento");
        }));

        assertTrue(outbox.findByReservaIdOrderByIdAsc(reservaId.get()).isEmpty(),
                "a linha do outbox foi gravada na mesma transação e deveria ter sumido com ela");
    }

    @Test
    void commitDaMudancaDeEstadoTerminaPublicadoEMarcado() {
        UUID reservaId = reservas.iniciar(new RealizarReservaCommand(CLIENTE, SESSAO, List.of("D2"), false, false)).reservaId();

        var linhas = outbox.findByReservaIdOrderByIdAsc(reservaId);
        assertEquals(1, linhas.size());
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(15)).until(
                () -> outbox.findByEventId(linhas.getFirst().getEventId()).orElseThrow().getPublicadoEm() != null);
    }

    @Test
    void publicarForaDeTransacaoEhErroDeProgramacao() {
        var envelope = new EventoEnvelope(UUID.randomUUID(), "Teste", 1, UUID.randomUUID(), 1, Instant.now(), "corr-teste-0001",
                "teste", JsonNodeFactory.instance.objectNode());
        assertThrows(IllegalTransactionStateException.class, () -> publicador.publicar(Topicos.RESERVA_EVENTOS, envelope));
    }
}
