package br.com.cinepass.notificacao.application;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.notificacao.domain.model.Destinatario;
import br.com.cinepass.notificacao.domain.model.DestinatarioSemContatoException;
import br.com.cinepass.notificacao.domain.model.Email;
import br.com.cinepass.notificacao.domain.model.ModeloDeMensagem;
import br.com.cinepass.notificacao.domain.model.Notificacao;
import br.com.cinepass.notificacao.domain.repository.DestinatarioRepository;
import br.com.cinepass.notificacao.domain.repository.NotificacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoApplicationService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoApplicationService.class);

    private final NotificacaoRepository notificacoes;
    private final DestinatarioRepository destinatarios;
    private final JsonMapper jsonMapper;

    public NotificacaoApplicationService(NotificacaoRepository notificacoes, DestinatarioRepository destinatarios,
                                         JsonMapper jsonMapper) {
        this.notificacoes = notificacoes;
        this.destinatarios = destinatarios;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Efeito de um evento: registrar a notificação correspondente. Roda dentro da transação
     * aberta pelo consumidor idempotente, junto com o registro do evento na inbox.
     */
    public void registrar(EventoEnvelope evento) {
        DadosDoEvento dados = jsonMapper.treeToValue(evento.payload(), DadosDoEvento.class);
        var conteudo = ModeloDeMensagem.para(evento.eventType(),
                new ModeloDeMensagem.Dados(dados.assentos(), dados.valorDoEvento(), dados.motivo(), dados.pagamentoEstornado()));
        if (conteudo.isEmpty()) {
            log.info("Evento {} sem notificação associada; nada a registrar", evento.eventType());
            return;
        }

        Destinatario destinatario = destinatarios.buscar(dados.clienteId())
                .orElseThrow(() -> new DestinatarioSemContatoException(dados.clienteId()));
        Notificacao notificacao = notificacoes.salvar(Notificacao.porEmail(evento.eventId(), evento.eventType(),
                evento.reservaId(), destinatario, conteudo.get(), evento.correlationId()));

        log.info("Notificação registrada: destinatario={} cliente={} canal={} assunto=\"{}\" resultado=REGISTRADA",
                notificacao.destinatario(), notificacao.clienteId(), notificacao.canal(), notificacao.assunto());
    }

    @Transactional(readOnly = true)
    public List<Notificacao> daReserva(UUID reservaId) {
        return notificacoes.daReserva(reservaId);
    }

    @Transactional(readOnly = true)
    public List<Notificacao> ultimas() {
        return notificacoes.ultimas();
    }

    @Transactional
    public Destinatario cadastrarDestinatario(UUID clienteId, String nome, String email) {
        Destinatario destinatario = destinatarios.salvar(new Destinatario(clienteId, nome, new Email(email)));
        log.info("Contato cadastrado para o cliente {}: {}", clienteId, destinatario.email().mascarado());
        return destinatario;
    }

    @Transactional(readOnly = true)
    public List<Destinatario> destinatarios() {
        return destinatarios.listar();
    }

    /** O que este serviço lê do payload. Campos que não usa são ignorados (leitor tolerante). */
    record DadosDoEvento(UUID clienteId, List<String> assentos, BigDecimal valorTotal, BigDecimal valor,
                         String motivo, Boolean pagamentoEstornado) {
        DadosDoEvento {
            // Campos ausentes em alguns tipos de evento chegam nulos; o Jackson 3 recusa
            // null em tipo primitivo, então o booleano é objeto e ganha um padrão aqui.
            assentos = assentos == null ? List.of() : assentos;
            pagamentoEstornado = Boolean.TRUE.equals(pagamentoEstornado);
        }

        BigDecimal valorDoEvento() {
            return valor != null ? valor : valorTotal;
        }
    }
}
