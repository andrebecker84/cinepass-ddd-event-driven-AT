package br.com.cinepass.auditoria.infrastructure.mensageria;

import br.com.cinepass.auditoria.application.AuditoriaApplicationService;
import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.inbox.ConsumidorIdempotente;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OuvinteDeEventosDeReserva {

    private final ConsumidorIdempotente consumidor;
    private final AuditoriaApplicationService auditoria;

    public OuvinteDeEventosDeReserva(ConsumidorIdempotente consumidor, AuditoriaApplicationService auditoria) {
        this.consumidor = consumidor;
        this.auditoria = auditoria;
    }

    @KafkaListener(topics = Topicos.RESERVA_EVENTOS)
    public void receber(ConsumerRecord<String, String> registro) {
        // O traceId vem do span de consumo que o Micrometer abriu a partir do cabeçalho do registro.
        var origem = new AuditoriaApplicationService.Origem(registro.topic(), registro.partition(), registro.offset(),
                MDC.get("traceId"));
        consumidor.processar(registro, evento -> auditoria.registrar(evento, origem));
    }
}
