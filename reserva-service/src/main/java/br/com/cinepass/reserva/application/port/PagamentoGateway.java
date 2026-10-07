package br.com.cinepass.reserva.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface PagamentoGateway {
    PagamentoResultado cobrar(UUID reservaId, BigDecimal valor, boolean simularRecusa);
    PagamentoResultado estornar(
            UUID pagamentoId
    );
    record PagamentoResultado(UUID pagamentoId, String status) {}
}
