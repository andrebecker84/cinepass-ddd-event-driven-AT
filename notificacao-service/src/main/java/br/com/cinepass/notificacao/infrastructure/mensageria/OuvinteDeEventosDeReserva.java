package br.com.cinepass.notificacao.infrastructure.mensageria;

import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.inbox.ConsumidorIdempotente;
import br.com.cinepass.notificacao.application.NotificacaoApplicationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome o ciclo de vida da reserva. O grupo e a concorrência vêm do {@code application.yml}
 * ({@code group-id: notificacao-service}, {@code concurrency: 3}): uma thread por partição,
 * reservas diferentes em paralelo, eventos da mesma reserva em sequência.
 */
@Component
public class OuvinteDeEventosDeReserva {

    private final ConsumidorIdempotente consumidor;
    private final NotificacaoApplicationService notificacoes;

    public OuvinteDeEventosDeReserva(ConsumidorIdempotente consumidor, NotificacaoApplicationService notificacoes) {
        this.consumidor = consumidor;
        this.notificacoes = notificacoes;
    }

    @KafkaListener(topics = Topicos.RESERVA_EVENTOS)
    public void receber(ConsumerRecord<String, String> registro) {
        consumidor.processar(registro, notificacoes::registrar);
    }
}
