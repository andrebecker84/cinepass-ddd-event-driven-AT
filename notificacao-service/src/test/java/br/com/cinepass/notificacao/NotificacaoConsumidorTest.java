package br.com.cinepass.notificacao;

import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.inbox.EventoProcessadoRepository;
import br.com.cinepass.mensageria.inbox.ReprocessadorDeDlt;
import br.com.cinepass.notificacao.application.NotificacaoApplicationService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
class NotificacaoConsumidorTest {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired JsonMapper json;
    @Autowired NotificacaoApplicationService notificacoes;
    @Autowired EventoProcessadoRepository inbox;
    @Autowired ReprocessadorDeDlt reprocessador;
    @Autowired KafkaListenerEndpointRegistry ouvintes;

    @BeforeEach
    void consumidorPronto() {
        EventosDeTeste.aguardarParticoesAtribuidas(ouvintes);
    }

    @Test
    void registraANotificacaoComODestinatarioMascarado() {
        UUID reservaId = UUID.randomUUID();
        EventosDeTeste.publicar(kafka, json, EventosDeTeste.evento(json, "ReservaCriada", reservaId, 1, EventosDeTeste.MARIA,
                Map.of("assentos", List.of("A1", "A2"), "valorTotal", new BigDecimal("79.80"))));

        await().atMost(Duration.ofSeconds(20)).until(() -> notificacoes.daReserva(reservaId).size() == 1);
        var notificacao = notificacoes.daReserva(reservaId).getFirst();
        assertEquals("m***@example.com", notificacao.destinatario());
        assertEquals("Recebemos sua reserva", notificacao.assunto());
        assertTrue(notificacao.mensagem().contains("A1, A2"));
    }

    @Test
    void aMesmaMensagemDuasVezesGeraUmaUnicaNotificacao() {
        UUID reservaId = UUID.randomUUID();
        var evento = EventosDeTeste.confirmada(json, reservaId, 3, EventosDeTeste.MARIA, "39.90");
        EventosDeTeste.publicar(kafka, json, evento);
        EventosDeTeste.publicar(kafka, json, evento);
        // Um terceiro evento da mesma reserva marca o fim: quando ele chega, a duplicata já passou.
        var marcador = EventosDeTeste.evento(json, "ReservaCancelada", reservaId, 4, EventosDeTeste.MARIA,
                Map.of("motivo", "PAGAMENTO_RECUSADO", "valorTotal", new BigDecimal("39.90")));
        EventosDeTeste.publicar(kafka, json, marcador);

        await().atMost(Duration.ofSeconds(20)).until(() -> inbox.existsById(marcador.eventId()));
        assertEquals(1, notificacoes.daReserva(reservaId).stream().filter(n -> n.eventId().equals(evento.eventId())).count(),
                "a reentrega não pode gerar uma segunda notificação");
        assertEquals(2, notificacoes.daReserva(reservaId).size());
    }

    @Test
    void falhaPersistenteVaiParaADltEPodeSerReprocessadaDepoisDaCorrecao() {
        UUID reservaId = EventosDeTeste.reservaNaParticao(1);
        var evento = EventosDeTeste.evento(json, "ReservaCriada", reservaId, 1, EventosDeTeste.SEM_CONTATO,
                Map.of("assentos", List.of("E9"), "valorTotal", new BigDecimal("39.90")));
        EventosDeTeste.publicar(kafka, json, evento);

        String dlt = Topicos.dltDe(Topicos.RESERVA_EVENTOS, "notificacao");
        var naDlt = lerDaDlt(dlt, evento.chave());
        assertEquals(1, naDlt.size(), "esgotadas as tentativas, a mensagem fica disponível na DLT");
        assertEquals(1, naDlt.getFirst().partition(), "a DLT preserva a partição de origem");
        assertTrue(notificacoes.daReserva(reservaId).isEmpty());
        assertFalse(inbox.existsById(evento.eventId()), "falha não registra o evento como processado");

        // Mensagem não relacionada, publicada depois, não fica presa atrás da que falhou.
        UUID outra = EventosDeTeste.reservaNaParticao(1);
        EventosDeTeste.publicar(kafka, json, EventosDeTeste.confirmada(json, outra, 3, EventosDeTeste.MARIA, "39.90"));
        await().atMost(Duration.ofSeconds(20)).until(() -> notificacoes.daReserva(outra).size() == 1);

        notificacoes.cadastrarDestinatario(EventosDeTeste.SEM_CONTATO, "Ana Lima", "ana.lima@example.com");
        ReprocessadorDeDlt.Resultado resultado = reprocessador.reprocessar(notificacoes::registrar);

        assertNull(resultado.falha());
        assertTrue(resultado.reprocessadas() >= 1);
        assertEquals(1, notificacoes.daReserva(reservaId).size());
        assertEquals("a***@example.com", notificacoes.daReserva(reservaId).getFirst().destinatario());
        assertEquals(0, reprocessador.reprocessar(notificacoes::registrar).lidas(), "o que foi reprocessado não volta");
    }

    private static List<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> lerDaDlt(String topico, String chave) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, InfraDeTeste.KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "teste-dlt-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> encontrados = new ArrayList<>();
        try (var consumidor = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumidor.subscribe(List.of(topico));
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                consumidor.poll(Duration.ofMillis(300)).forEach(r -> {
                    if (chave.equals(r.key())) encontrados.add(r);
                });
                return !encontrados.isEmpty();
            });
        }
        return encontrados;
    }
}
