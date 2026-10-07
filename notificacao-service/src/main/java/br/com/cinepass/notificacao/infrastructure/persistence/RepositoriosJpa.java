package br.com.cinepass.notificacao.infrastructure.persistence;

import br.com.cinepass.notificacao.domain.model.Destinatario;
import br.com.cinepass.notificacao.domain.model.Email;
import br.com.cinepass.notificacao.domain.model.Notificacao;
import br.com.cinepass.notificacao.domain.repository.DestinatarioRepository;
import br.com.cinepass.notificacao.domain.repository.NotificacaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptadores JPA das duas portas de repositório do domínio de notificação. */
final class RepositoriosJpa {

    private RepositoriosJpa() {
    }

    @Repository
    static class Notificacoes implements NotificacaoRepository {
        private final SpringDataNotificacaoRepository repository;
        Notificacoes(SpringDataNotificacaoRepository repository) { this.repository = repository; }

        @Override public Notificacao salvar(Notificacao n) {
            repository.save(new NotificacaoJpaEntity(n.id(), n.eventId(), n.tipoEvento(), n.reservaId(), n.clienteId(), n.canal(),
                    n.destinatario(), n.assunto(), n.mensagem(), n.correlationId(), n.registradaEm()));
            return n;
        }
        @Override public List<Notificacao> daReserva(UUID reservaId) {
            return repository.findByReservaIdOrderByRegistradaEmAsc(reservaId).stream().map(Notificacoes::paraDominio).toList();
        }
        @Override public List<Notificacao> ultimas() {
            return repository.findTop50ByOrderByRegistradaEmDesc().stream().map(Notificacoes::paraDominio).toList();
        }
        private static Notificacao paraDominio(NotificacaoJpaEntity e) {
            return new Notificacao(e.getId(), e.getEventId(), e.getTipoEvento(), e.getReservaId(), e.getClienteId(), e.getCanal(),
                    e.getDestinatario(), e.getAssunto(), e.getMensagem(), e.getCorrelationId(), e.getRegistradaEm());
        }
    }

    @Repository
    static class Destinatarios implements DestinatarioRepository {
        private final SpringDataDestinatarioRepository repository;
        Destinatarios(SpringDataDestinatarioRepository repository) { this.repository = repository; }

        @Override public Optional<Destinatario> buscar(UUID clienteId) { return repository.findById(clienteId).map(Destinatarios::paraDominio); }
        @Override public Destinatario salvar(Destinatario d) {
            repository.save(new DestinatarioJpaEntity(d.clienteId(), d.nome(), d.email().valor()));
            return d;
        }
        @Override public List<Destinatario> listar() { return repository.findAll().stream().map(Destinatarios::paraDominio).toList(); }
        private static Destinatario paraDominio(DestinatarioJpaEntity e) {
            return new Destinatario(e.getClienteId(), e.getNome(), new Email(e.getEmail()));
        }
    }
}
