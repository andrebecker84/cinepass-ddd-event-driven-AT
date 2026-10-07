package br.com.cinepass.reputacao.infrastructure.mensageria;

import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.inbox.ConsumidorIdempotente;
import br.com.cinepass.reputacao.application.ReputacaoApplicationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OuvinteDeEventosDeReserva {

    private final ConsumidorIdempotente consumidor;
    private final ReputacaoApplicationService reputacao;

    public OuvinteDeEventosDeReserva(ConsumidorIdempotente consumidor, ReputacaoApplicationService reputacao) {
        this.consumidor = consumidor;
        this.reputacao = reputacao;
    }

    @KafkaListener(topics = Topicos.RESERVA_EVENTOS)
    public void receber(ConsumerRecord<String, String> registro) {
        consumidor.processar(registro, reputacao::aplicar);
    }
}
