package br.com.cinepass.reputacao.application;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.reputacao.domain.model.RegistroHistorico;
import br.com.cinepass.reputacao.domain.model.ReputacaoCliente;
import br.com.cinepass.reputacao.domain.repository.ReputacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReputacaoApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReputacaoApplicationService.class);

    private final ReputacaoRepository repositorio;
    private final JsonMapper jsonMapper;

    public ReputacaoApplicationService(ReputacaoRepository repositorio, JsonMapper jsonMapper) {
        this.repositorio = repositorio;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Aplica um evento à reputação do cliente. Só a confirmação e o cancelamento têm impacto;
     * a criação e a aprovação do pagamento são etapas intermediárias e não mudam nada aqui.
     * Roda na transação do consumidor idempotente, junto com o registro na inbox.
     */
    public void aplicar(EventoEnvelope evento) {
        switch (evento.eventType()) {
            case "ReservaConfirmada" -> confirmar(evento);
            case "ReservaCancelada" -> cancelar(evento);
            default -> log.info("Evento {} sem impacto na reputação; nada alterado", evento.eventType());
        }
    }

    private void confirmar(EventoEnvelope evento) {
        Dados dados = jsonMapper.treeToValue(evento.payload(), Dados.class);
        ReputacaoCliente reputacao = carregarParaAtualizar(dados.clienteId());
        long pontos = reputacao.registrarReservaConfirmada(dados.valorTotal());
        repositorio.salvar(reputacao);
        repositorio.registrar(new RegistroHistorico(evento.eventId(), dados.clienteId(), evento.reservaId(), evento.eventType(),
                "Reserva confirmada (" + dados.valorTotal() + ")", pontos, evento.occurredAt(), evento.correlationId()));
        log.info("Reputação atualizada: cliente={} confirmadas={} pontos={} (+{}) nivel={}",
                dados.clienteId(), reputacao.getReservasConfirmadas(), reputacao.getPontos(), pontos, reputacao.nivel());
    }

    private void cancelar(EventoEnvelope evento) {
        Dados dados = jsonMapper.treeToValue(evento.payload(), Dados.class);
        ReputacaoCliente reputacao = carregarParaAtualizar(dados.clienteId());
        boolean penalizou = reputacao.registrarCancelamento(dados.motivo());
        repositorio.salvar(reputacao);
        String descricao = penalizou
                ? "Cancelada por pagamento recusado"
                : "Cancelada por falha da plataforma (" + dados.motivo() + "), sem penalidade";
        repositorio.registrar(new RegistroHistorico(evento.eventId(), dados.clienteId(), evento.reservaId(), evento.eventType(),
                descricao, 0, evento.occurredAt(), evento.correlationId()));
        log.info("Reputação atualizada: cliente={} canceladas={} pagamentosRecusados={} penalidade={}",
                dados.clienteId(), reputacao.getReservasCanceladas(), reputacao.getPagamentosRecusados(), penalizou);
    }

    private ReputacaoCliente carregarParaAtualizar(UUID clienteId) {
        return repositorio.carregarParaAtualizar(clienteId);
    }

    @Transactional(readOnly = true)
    public Optional<ReputacaoCliente> reputacao(UUID clienteId) {
        return repositorio.buscar(clienteId);
    }

    @Transactional(readOnly = true)
    public List<RegistroHistorico> historico(UUID clienteId) {
        return repositorio.historico(clienteId);
    }

    @Transactional(readOnly = true)
    public List<ReputacaoCliente> todas() {
        return repositorio.listar();
    }

    record Dados(UUID clienteId, BigDecimal valorTotal, String motivo) {
    }
}
