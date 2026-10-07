package br.com.cinepass.notificacao.domain.repository;

import br.com.cinepass.notificacao.domain.model.Destinatario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinatarioRepository {
    Optional<Destinatario> buscar(UUID clienteId);
    Destinatario salvar(Destinatario destinatario);
    List<Destinatario> listar();
}
