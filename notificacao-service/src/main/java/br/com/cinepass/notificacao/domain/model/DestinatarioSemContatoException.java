package br.com.cinepass.notificacao.domain.model;

import java.util.UUID;

/**
 * O cliente não tem contato cadastrado. A notificação não pode ser registrada agora, mas
 * poderá depois que o contato for cadastrado: a mensagem vai para a DLT e fica disponível
 * para reprocessamento.
 */
public class DestinatarioSemContatoException extends RuntimeException {
    public DestinatarioSemContatoException(UUID clienteId) {
        super("Cliente " + clienteId + " sem contato cadastrado para notificação");
    }
}
