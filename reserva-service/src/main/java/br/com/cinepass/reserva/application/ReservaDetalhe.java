package br.com.cinepass.reserva.application;

import br.com.cinepass.reserva.domain.model.Reserva;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservaDetalhe(UUID reservaId, UUID clienteId, UUID sessaoId, List<String> assentos, BigDecimal valorTotal,
                             String status, String motivoCancelamento, UUID pagamentoId, UUID ingressoId, Instant criadaEm) {
    public static ReservaDetalhe de(Reserva r) {
        return new ReservaDetalhe(r.getId().valor(), r.getClienteId().valor(), r.getSessaoId().valor(),
                r.getAssentos().stream().map(a -> a.valor()).toList(), r.getValorTotal().valor(), r.getStatus().name(),
                r.getMotivoCancelamento() == null ? null : r.getMotivoCancelamento().name(), r.getPagamentoId(), r.getIngressoId(), r.getCriadaEm());
    }
}
