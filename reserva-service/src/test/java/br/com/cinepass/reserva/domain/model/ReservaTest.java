package br.com.cinepass.reserva.domain.model;

import br.com.cinepass.reserva.domain.event.EventoDeReserva;
import br.com.cinepass.reserva.domain.event.PagamentoConfirmado;
import br.com.cinepass.reserva.domain.event.ReservaCancelada;
import br.com.cinepass.reserva.domain.event.ReservaConfirmada;
import br.com.cinepass.reserva.domain.event.ReservaCriada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReservaTest {

    private static Reserva novaReserva() {
        return Reserva.criar(new ClienteId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()),
                List.of(new AssentoId("A1")), new Dinheiro(new BigDecimal("39.90")));
    }

    @Test
    void devePercorrerFluxoFeliz() {
        var reserva = novaReserva();
        reserva.aguardarPagamento();
        reserva.confirmarPagamento(UUID.randomUUID());
        reserva.iniciarEmissaoIngresso();
        reserva.confirmar(UUID.randomUUID());
        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
    }

    @Test
    void cadaTransicaoRelevanteRegistraUmEventoNumeradoEmSequencia() {
        var reserva = novaReserva();
        List<EventoDeReserva> eventos = new ArrayList<>(reserva.puxarEventos());
        reserva.aguardarPagamento();
        reserva.confirmarPagamento(UUID.randomUUID());
        reserva.iniciarEmissaoIngresso();
        eventos.addAll(reserva.puxarEventos());
        reserva.confirmar(UUID.randomUUID());
        eventos.addAll(reserva.puxarEventos());

        assertEquals(List.of(ReservaCriada.class, PagamentoConfirmado.class, ReservaConfirmada.class),
                eventos.stream().map(Object::getClass).toList());
        assertEquals(List.of(1L, 2L, 3L), eventos.stream().map(EventoDeReserva::sequencia).toList());
        assertEquals(3, eventos.stream().map(EventoDeReserva::eventId).distinct().count(), "cada evento tem seu próprio eventId");
        assertTrue(eventos.stream().allMatch(e -> e.reservaId().equals(reserva.getId())));
        assertEquals(3, reserva.getSequenciaEventos());
    }

    @Test
    void puxarEventosEntregaCadaEventoUmaUnicaVez() {
        var reserva = novaReserva();
        assertEquals(1, reserva.puxarEventos().size());
        assertTrue(reserva.puxarEventos().isEmpty());
    }

    @Test
    void cancelarDuasVezesEmiteUmUnicoEvento() {
        var reserva = novaReserva();
        reserva.puxarEventos();
        reserva.aguardarPagamento();
        reserva.cancelar(MotivoCancelamento.PAGAMENTO_RECUSADO, UUID.randomUUID(), false);
        reserva.cancelar(MotivoCancelamento.PAGAMENTO_RECUSADO, UUID.randomUUID(), false);

        var eventos = reserva.puxarEventos();
        assertEquals(1, eventos.size());
        var cancelada = assertInstanceOf(ReservaCancelada.class, eventos.getFirst());
        assertEquals(MotivoCancelamento.PAGAMENTO_RECUSADO, cancelada.motivo());
        assertEquals(2, cancelada.sequencia());
    }

    @Test
    void reservaConfirmadaNaoPodeSerCanceladaPelaSaga() {
        var reserva = novaReserva();
        reserva.aguardarPagamento();
        reserva.confirmarPagamento(UUID.randomUUID());
        reserva.iniciarEmissaoIngresso();
        reserva.confirmar(UUID.randomUUID());
        assertThrows(IllegalStateException.class,
                () -> reserva.cancelar(MotivoCancelamento.FALHA_EMISSAO_INGRESSO, null, false));
    }

    @Test
    void aSequenciaContinuaDepoisDeRestaurada() {
        var restaurada = Reserva.restaurar(ReservaId.novo(), new ClienteId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()),
                List.of(new AssentoId("B2")), new Dinheiro(BigDecimal.TEN), StatusReserva.AGUARDANDO_PAGAMENTO,
                null, null, null, 1, java.time.Instant.now());
        restaurada.confirmarPagamento(UUID.randomUUID());
        assertEquals(2, restaurada.puxarEventos().getFirst().sequencia());
    }
}
