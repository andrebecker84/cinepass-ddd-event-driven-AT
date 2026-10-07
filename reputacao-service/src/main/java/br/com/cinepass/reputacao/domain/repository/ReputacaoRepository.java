package br.com.cinepass.reputacao.domain.repository;

import br.com.cinepass.reputacao.domain.model.RegistroHistorico;
import br.com.cinepass.reputacao.domain.model.ReputacaoCliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReputacaoRepository {

    /**
     * Carrega a reputação já bloqueada para escrita, criando-a zerada se o cliente ainda não
     * tem. A chave de partição do Kafka é a reserva, não o cliente: duas reservas do mesmo cliente podem ser processadas ao mesmo
     * tempo por threads diferentes, e sem o bloqueio uma atualização apagaria a outra.
     */
    ReputacaoCliente carregarParaAtualizar(UUID clienteId);

    Optional<ReputacaoCliente> buscar(UUID clienteId);

    List<ReputacaoCliente> listar();

    void salvar(ReputacaoCliente reputacao);

    void registrar(RegistroHistorico registro);

    List<RegistroHistorico> historico(UUID clienteId);
}
