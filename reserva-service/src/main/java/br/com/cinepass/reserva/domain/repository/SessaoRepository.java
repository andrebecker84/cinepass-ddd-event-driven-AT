package br.com.cinepass.reserva.domain.repository;

import br.com.cinepass.reserva.domain.model.Sessao;
import br.com.cinepass.reserva.domain.model.SessaoId;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository {
    Optional<Sessao> buscarPorId(SessaoId id);

    /**
     * Carrega a sessão bloqueada para escrita até o fim da transação. Reservas simultâneas na
     * mesma sessão, mesmo em assentos diferentes, alteram a mesma linha: sem o bloqueio, a
     * versão otimista faria todas menos uma falharem; com ele, elas esperam a vez, por
     * milissegundos, porque a transação do passo é curta e não tem chamada remota dentro.
     */
    Optional<Sessao> buscarPorIdParaAtualizar(SessaoId id);
    Sessao salvar(Sessao sessao);
    List<Sessao> listar();
}
