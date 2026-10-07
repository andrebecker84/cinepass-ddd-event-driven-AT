package br.com.cinepass.reputacao.infrastructure.persistence;

import br.com.cinepass.reputacao.domain.model.RegistroHistorico;
import br.com.cinepass.reputacao.domain.model.ReputacaoCliente;
import br.com.cinepass.reputacao.domain.repository.ReputacaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReputacaoRepositoryJpaAdapter implements ReputacaoRepository {

    private final SpringDataReputacaoRepository reputacoes;
    private final SpringDataHistoricoRepository historico;

    public ReputacaoRepositoryJpaAdapter(SpringDataReputacaoRepository reputacoes, SpringDataHistoricoRepository historico) {
        this.reputacoes = reputacoes;
        this.historico = historico;
    }

    /**
     * Garante a linha e depois a bloqueia ({@code select ... for update}). As duas etapas
     * eliminam a corrida da primeira reserva do cliente: sem linha não há o que bloquear, e
     * duas threads criariam a reputação ao mesmo tempo, uma apagando a outra.
     */
    @Override
    public ReputacaoCliente carregarParaAtualizar(UUID clienteId) {
        reputacoes.garantirExistencia(clienteId);
        return reputacoes.bloquear(clienteId).map(ReputacaoRepositoryJpaAdapter::paraDominio).orElseThrow();
    }

    @Override
    public Optional<ReputacaoCliente> buscar(UUID clienteId) {
        return reputacoes.findById(clienteId).map(ReputacaoRepositoryJpaAdapter::paraDominio);
    }

    @Override
    public List<ReputacaoCliente> listar() {
        return reputacoes.findAll().stream().map(ReputacaoRepositoryJpaAdapter::paraDominio).toList();
    }

    /**
     * A entidade já está no contexto de persistência, carregada com bloqueio por esta mesma
     * transação: o {@code findById} não vai ao banco, só devolve a instância gerenciada.
     */
    @Override
    public void salvar(ReputacaoCliente r) {
        ReputacaoJpaEntity entidade = reputacoes.findById(r.getClienteId()).orElseThrow();
        entidade.atualizar(r.getReservasConfirmadas(), r.getReservasCanceladas(), r.getPagamentosRecusados(), r.getPontos(),
                r.getValorTotalGasto(), r.nivel().name(), r.getAtualizadaEm());
        reputacoes.save(entidade);
    }

    @Override
    public void registrar(RegistroHistorico h) {
        historico.save(new HistoricoJpaEntity(h.eventId(), h.clienteId(), h.reservaId(), h.tipoEvento(), h.descricao(),
                h.pontos(), h.ocorridoEm(), h.correlationId()));
    }

    @Override
    public List<RegistroHistorico> historico(UUID clienteId) {
        return historico.findByClienteIdOrderByOcorridoEmAsc(clienteId).stream()
                .map(e -> new RegistroHistorico(e.getEventId(), e.getClienteId(), e.getReservaId(), e.getTipoEvento(),
                        e.getDescricao(), e.getPontos(), e.getOcorridoEm(), e.getCorrelationId()))
                .toList();
    }

    private static ReputacaoCliente paraDominio(ReputacaoJpaEntity e) {
        return ReputacaoCliente.restaurar(e.getClienteId(), e.getReservasConfirmadas(), e.getReservasCanceladas(),
                e.getPagamentosRecusados(), e.getPontos(), e.getValorTotalGasto(), e.getAtualizadaEm());
    }
}
