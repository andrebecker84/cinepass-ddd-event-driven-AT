package br.com.cinepass.notificacao.domain.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * O texto de cada notificação, por tipo de evento. Tipo desconhecido não é erro: devolve
 * vazio, e o evento é registrado como processado sem notificar (leitor tolerante). Assim
 * um tipo novo publicado pelo reserva-service não derruba este consumidor.
 */
public final class ModeloDeMensagem {

    public record Conteudo(String assunto, String mensagem) {
    }

    public record Dados(List<String> assentos, BigDecimal valor, String motivo, boolean pagamentoEstornado) {
    }

    private ModeloDeMensagem() {
    }

    public static Optional<Conteudo> para(String tipoEvento, Dados d) {
        return Optional.ofNullable(switch (tipoEvento) {
            case "ReservaCriada" -> new Conteudo("Recebemos sua reserva",
                    "Sua reserva dos assentos " + String.join(", ", d.assentos()) + " (" + reais(d.valor())
                            + ") foi recebida e aguarda a confirmação do pagamento.");
            case "PagamentoConfirmado" -> new Conteudo("Pagamento aprovado",
                    "O pagamento de " + reais(d.valor()) + " foi aprovado. Estamos emitindo seu ingresso.");
            case "ReservaConfirmada" -> new Conteudo("Ingresso emitido",
                    "Sua reserva está confirmada para os assentos " + String.join(", ", d.assentos()) + ". Bom filme!");
            case "ReservaCancelada" -> new Conteudo("Reserva cancelada", textoDeCancelamento(d));
            default -> null;
        });
    }

    private static String textoDeCancelamento(Dados d) {
        String motivo = switch (d.motivo() == null ? "" : d.motivo()) {
            case "PAGAMENTO_RECUSADO" -> "o pagamento não foi aprovado";
            case "FALHA_EMISSAO_INGRESSO" -> "não foi possível emitir o ingresso";
            case "FALHA_COMUNICACAO_PAGAMENTO" -> "o pagamento não pôde ser processado";
            default -> "houve um problema no processamento";
        };
        return "Sua reserva foi cancelada porque " + motivo + "."
                + (d.pagamentoEstornado() ? " O valor de " + reais(d.valor()) + " foi estornado." : "")
                + " Os assentos voltaram a ficar disponíveis.";
    }

    private static String reais(BigDecimal valor) {
        return valor == null ? "valor não informado" : NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }
}
