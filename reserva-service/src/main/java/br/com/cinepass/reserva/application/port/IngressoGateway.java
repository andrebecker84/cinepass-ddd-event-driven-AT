package br.com.cinepass.reserva.application.port;

import java.util.List;
import java.util.UUID;

public interface IngressoGateway {
    IngressoResultado emitir(UUID reservaId, UUID sessaoId, List<String> assentos, boolean simularFalha);
    record IngressoResultado(UUID ingressoId, String codigo, String status) {}
}
