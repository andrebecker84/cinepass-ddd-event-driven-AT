package br.com.cinepass.reserva.application;

import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import br.com.cinepass.reserva.application.port.IngressoGateway;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import br.com.cinepass.reserva.application.port.ReservaOrquestrador;
import br.com.cinepass.reserva.domain.model.MotivoCancelamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Saga orquestrada da reserva: a mesma sequência de passos e compensações do workflow
 * Temporal da base, agora sem servidor externo.
 *
 * <pre>
 *   iniciar ─► cobrar ─► confirmarPagamento ─► emitir ingresso ─► confirmar
 *                │                                  │
 *           recusado/falha                        falha
 *                ▼                                  ▼
 *             cancelar                   estornar pagamento ─► cancelar
 * </pre>
 *
 * <p>Este método não é transacional, e não pode ser: cada passo local commita sozinho,
 * antes da próxima chamada remota. O estado da Saga é o próprio {@code StatusReserva},
 * persistido a cada passo.
 *
 * <p>Pagamento e ingresso são participantes da Saga e continuam sendo chamados por HTTP,
 * como na base. Os serviços auxiliares (notificação, reputação, auditoria) não são chamados
 * por ninguém: reagem aos eventos que cada passo gravou no outbox.
 */
@Service
public class SagaDeReserva implements ReservaOrquestrador {

    private static final Logger log = LoggerFactory.getLogger(SagaDeReserva.class);

    private final ReservaApplicationService reservas;
    private final PagamentoGateway pagamentoGateway;
    private final IngressoGateway ingressoGateway;

    public SagaDeReserva(ReservaApplicationService reservas, PagamentoGateway pagamentoGateway, IngressoGateway ingressoGateway) {
        this.reservas = reservas;
        this.pagamentoGateway = pagamentoGateway;
        this.ingressoGateway = ingressoGateway;
    }

    @Override
    public ReservaDetalhe realizar(RealizarReservaCommand command) {
        ReservaDetalhe reserva = reservas.iniciar(command);
        UUID reservaId = reserva.reservaId();
        try (var mdc = MdcDoEvento.de(reservaId, null, null, null)) {
            log.info("Saga iniciada: reserva criada com {} assento(s), valor {}", reserva.assentos().size(), reserva.valorTotal());

            PagamentoGateway.PagamentoResultado pagamento;
            try {
                pagamento = pagamentoGateway.cobrar(reservaId, reserva.valorTotal(), command.simularRecusaPagamento());
            } catch (FalhaIntegracaoException e) {
                log.error("Pagamento indisponível, reserva será cancelada: {}", e.getMessage());
                reservas.cancelar(reservaId, MotivoCancelamento.FALHA_COMUNICACAO_PAGAMENTO, null, false);
                throw e;
            }

            if (!"APROVADO".equals(pagamento.status())) {
                log.warn("Pagamento {} recusado, reserva será cancelada", pagamento.pagamentoId());
                reservas.cancelar(reservaId, MotivoCancelamento.PAGAMENTO_RECUSADO, pagamento.pagamentoId(), false);
                throw new PagamentoRecusadoException(reservaId);
            }

            reserva = reservas.confirmarPagamento(reservaId, pagamento.pagamentoId());
            log.info("Pagamento {} aprovado, emitindo ingresso", pagamento.pagamentoId());

            IngressoGateway.IngressoResultado ingresso;
            try {
                ingresso = ingressoGateway.emitir(reservaId, reserva.sessaoId(), reserva.assentos(), command.simularFalhaIngresso());
            } catch (FalhaIntegracaoException e) {
                log.error("Emissão do ingresso falhou, iniciando compensação: {}", e.getMessage());
                boolean estornado = estornar(pagamento.pagamentoId());
                reservas.cancelar(reservaId, MotivoCancelamento.FALHA_EMISSAO_INGRESSO, pagamento.pagamentoId(), estornado);
                throw new ReservaCanceladaException(reservaId, MotivoCancelamento.FALHA_EMISSAO_INGRESSO.name(),
                        pagamento.pagamentoId(), estornado);
            }

            ReservaDetalhe confirmada = reservas.confirmar(reservaId, ingresso.ingressoId());
            log.info("Saga concluída: reserva confirmada com ingresso {}", ingresso.codigo());
            return confirmada;
        }
    }

    /** Compensação do pagamento. Se o estorno falhar, a reserva é cancelada mesmo assim e o fato fica registrado. */
    private boolean estornar(UUID pagamentoId) {
        try {
            pagamentoGateway.estornar(pagamentoId);
            log.info("Compensação: pagamento {} estornado", pagamentoId);
            return true;
        } catch (FalhaIntegracaoException e) {
            log.error("Compensação incompleta: estorno do pagamento {} falhou e precisa de intervenção: {}", pagamentoId, e.getMessage());
            return false;
        }
    }
}
