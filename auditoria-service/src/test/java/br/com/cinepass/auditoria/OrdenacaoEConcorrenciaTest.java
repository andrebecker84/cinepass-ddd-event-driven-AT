package br.com.cinepass.auditoria;

import br.com.cinepass.auditoria.application.AuditoriaApplicationService;
import br.com.cinepass.auditoria.domain.repository.AuditoriaRepository;
import br.com.cinepass.mensageria.EventoEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * O requisito central do item 4: reservas diferentes em paralelo, eventos da mesma reserva
 * em ordem.
 *
 * <p>Três reservas, escolhidas para cair cada uma numa partição, publicam dez eventos cada,
 * intercalados (r1-1, r2-1, r3-1, r1-2...). O teste exige: cada reserva processada na ordem
 * 1..10, cada reserva por uma única thread, e três threads no total. Se o consumo fosse
 * sequencial, a última condição falharia; se a ordem não fosse garantida, a primeira.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InfraDeTeste.class)
class OrdenacaoEConcorrenciaTest {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired JsonMapper json;
    @Autowired AuditoriaRepository auditoria;
    @Autowired KafkaListenerEndpointRegistry ouvintes;
    @MockitoSpyBean AuditoriaApplicationService servico;

    private final Map<UUID, List<Long>> ordemPorReserva = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> threadsPorReserva = new ConcurrentHashMap<>();

    @BeforeEach
    void observarProcessamento() {
        doAnswer(chamada -> {
            EventoEnvelope evento = chamada.getArgument(0);
            ordemPorReserva.computeIfAbsent(evento.reservaId(), k -> new CopyOnWriteArrayList<>()).add(evento.sequence());
            threadsPorReserva.computeIfAbsent(evento.reservaId(), k -> ConcurrentHashMap.newKeySet()).add(Thread.currentThread().getName());
            return chamada.callRealMethod();
        }).when(servico).registrar(any(), any());
        EventosDeTeste.aguardarParticoesAtribuidas(ouvintes);
    }

    @Test
    void eventosDaMesmaReservaEmOrdemEReservasDiferentesEmParalelo() {
        List<UUID> reservas = List.of(EventosDeTeste.reservaNaParticao(0), EventosDeTeste.reservaNaParticao(1),
                EventosDeTeste.reservaNaParticao(2));
        for (long sequencia = 1; sequencia <= 10; sequencia++) {
            for (UUID reserva : reservas) {
                EventosDeTeste.publicar(kafka, json, EventosDeTeste.evento(json, "Evento" + sequencia, reserva, sequencia,
                        EventosDeTeste.MARIA, Map.of()));
            }
        }

        await().atMost(Duration.ofSeconds(30)).until(() -> reservas.stream()
                .allMatch(r -> ordemPorReserva.getOrDefault(r, List.of()).size() == 10));

        List<Long> esperado = LongStream.rangeClosed(1, 10).boxed().toList();
        Set<String> todasAsThreads = new HashSet<>();
        for (UUID reserva : reservas) {
            assertEquals(esperado, ordemPorReserva.get(reserva), "os eventos da reserva " + reserva + " chegaram fora de ordem");
            assertEquals(1, threadsPorReserva.get(reserva).size(), "uma reserva, uma thread");
            todasAsThreads.addAll(threadsPorReserva.get(reserva));

            var auditados = auditoria.consultar(new AuditoriaRepository.Filtro(reserva, null, null, null));
            assertEquals(1, auditados.stream().map(r -> r.particao()).distinct().count(), "uma partição por reserva");
            var offsets = auditados.stream().map(r -> r.offset()).toList();
            assertEquals(offsets.stream().sorted().toList(), offsets, "sequência e offset crescem juntos");
        }
        assertEquals(3, todasAsThreads.size(), "três partições, três threads trabalhando em paralelo: " + todasAsThreads);
    }

    @Test
    void consultaPorCorrelationIdEOMesmoEventoAuditadoUmaVezSo() {
        UUID reservaId = UUID.randomUUID();
        var evento = EventosDeTeste.evento(json, "ReservaCriada", reservaId, 1, EventosDeTeste.MARIA, Map.of());
        EventosDeTeste.publicar(kafka, json, evento);
        EventosDeTeste.publicar(kafka, json, evento);
        var marcador = EventosDeTeste.evento(json, "ReservaConfirmada", reservaId, 2, EventosDeTeste.MARIA, Map.of());
        EventosDeTeste.publicar(kafka, json, marcador);

        await().atMost(Duration.ofSeconds(20)).until(() -> auditoria.porEvento(marcador.eventId()).isPresent());
        var porCorrelacao = auditoria.consultar(new AuditoriaRepository.Filtro(null, evento.correlationId(), null, null));
        assertEquals(2, porCorrelacao.size(), "a duplicata não gerou um segundo registro");
        assertEquals(evento.correlationId(), porCorrelacao.getFirst().correlationId());
        assertNotNull(auditoria.porEvento(evento.eventId()).orElseThrow().payload());
    }
}
