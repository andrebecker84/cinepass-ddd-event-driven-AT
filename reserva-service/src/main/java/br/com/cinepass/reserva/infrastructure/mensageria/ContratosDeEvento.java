package br.com.cinepass.reserva.infrastructure.mensageria;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payloads publicados no Kafka: a linguagem publicada do contexto de Reserva, versão 1.
 *
 * <p>São distintos dos eventos de domínio de propósito. O domínio usa objetos de valor
 * ({@code ReservaId}, {@code Dinheiro}); o contrato usa tipos primitivos que qualquer
 * consumidor, em qualquer linguagem, lê sem conhecer o modelo do produtor. Se o domínio
 * mudar, o contrato só muda se esta tradução mudar, e a especificação em
 * {@code docs/EVENTOS.md} acompanha.
 *
 * <p>Todos carregam {@code clienteId}: é o destinatário da notificação e o titular da
 * reputação, e nenhum consumidor precisa perguntar ao reserva-service a quem o evento diz respeito.
 */
final class ContratosDeEvento {

    private ContratosDeEvento() {
    }

    record ReservaCriadaV1(UUID clienteId, UUID sessaoId, List<String> assentos, BigDecimal valorTotal) {
    }

    record PagamentoConfirmadoV1(UUID clienteId, UUID pagamentoId, BigDecimal valor) {
    }

    record ReservaConfirmadaV1(UUID clienteId, UUID sessaoId, List<String> assentos, BigDecimal valorTotal,
                               UUID pagamentoId, UUID ingressoId) {
    }

    record ReservaCanceladaV1(UUID clienteId, String motivo, BigDecimal valorTotal, UUID pagamentoId,
                              boolean pagamentoEstornado) {
    }
}
