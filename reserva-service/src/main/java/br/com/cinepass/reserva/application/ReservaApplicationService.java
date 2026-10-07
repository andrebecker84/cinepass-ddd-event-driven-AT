package br.com.cinepass.reserva.application;

import br.com.cinepass.reserva.application.port.PublicadorDeEventos;
import br.com.cinepass.reserva.domain.model.*;
import br.com.cinepass.reserva.domain.repository.ReservaRepository;
import br.com.cinepass.reserva.domain.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cada método é um passo da Saga em sua própria transação local, curta, sem chamada
 * remota dentro dela. É o oposto do {@code realizar()} da base, que mantinha a transação
 * aberta durante as chamadas HTTP de propósito, para mostrar que {@code @Transactional}
 * não controla os bancos dos outros serviços.
 *
 * <p>Todo passo termina em {@link #salvarEPublicar}: o novo estado e os eventos que ele
 * gerou vão para o banco na mesma transação (estado na tabela {@code reservas}, eventos
 * na tabela do outbox). Ou os dois são gravados, ou nenhum.
 */
@Service
public class ReservaApplicationService {
    private final ReservaRepository reservaRepository;
    private final SessaoRepository sessaoRepository;
    private final PublicadorDeEventos publicadorDeEventos;

    public ReservaApplicationService(ReservaRepository reservaRepository, SessaoRepository sessaoRepository,
                                     PublicadorDeEventos publicadorDeEventos) {
        this.reservaRepository=reservaRepository; this.sessaoRepository=sessaoRepository;
        this.publicadorDeEventos=publicadorDeEventos;
    }

    /** Passo 1: bloqueia os assentos e cria a reserva aguardando pagamento. Emite {@code ReservaCriada}. */
    @Transactional
    public ReservaDetalhe iniciar(RealizarReservaCommand command) {
        Sessao sessao = sessaoRepository.buscarPorIdParaAtualizar(new SessaoId(command.sessaoId()))
                .orElseThrow(() -> new SessaoNaoEncontradaException(command.sessaoId()));

        List<AssentoId> assentos = command.assentos().stream().map(AssentoId::new).toList();
        sessao.reservar(assentos);
        sessaoRepository.salvar(sessao);

        Reserva reserva = Reserva.criar(new ClienteId(command.clienteId()), sessao.getId(), assentos,
                sessao.getPreco().multiplicar(assentos.size()));
        reserva.aguardarPagamento();
        return ReservaDetalhe.de(salvarEPublicar(reserva));
    }

    /** Passo 2: registra o pagamento aprovado e abre a emissão do ingresso. Emite {@code PagamentoConfirmado}. */
    @Transactional
    public ReservaDetalhe confirmarPagamento(UUID reservaId, UUID pagamentoId) {
        Reserva reserva = carregar(reservaId);
        reserva.confirmarPagamento(pagamentoId);
        reserva.iniciarEmissaoIngresso();
        return ReservaDetalhe.de(salvarEPublicar(reserva));
    }

    /** Passo 3: ingresso emitido, reserva concluída. Emite {@code ReservaConfirmada}. */
    @Transactional
    public ReservaDetalhe confirmar(UUID reservaId, UUID ingressoId) {
        Reserva reserva = carregar(reservaId);
        reserva.confirmar(ingressoId);
        return ReservaDetalhe.de(salvarEPublicar(reserva));
    }

    /** Compensação local: devolve os assentos à sessão e encerra a reserva. Emite {@code ReservaCancelada}. */
    @Transactional
    public ReservaDetalhe cancelar(UUID reservaId, MotivoCancelamento motivo, UUID pagamentoId, boolean pagamentoEstornado) {
        Reserva reserva = carregar(reservaId);
        Sessao sessao = sessaoRepository.buscarPorIdParaAtualizar(reserva.getSessaoId())
                .orElseThrow(() -> new SessaoNaoEncontradaException(reserva.getSessaoId().valor()));
        if (reserva.getStatus() != StatusReserva.CANCELADA) {
            sessao.liberar(reserva.getAssentos());
            sessaoRepository.salvar(sessao);
        }
        reserva.cancelar(motivo, pagamentoId, pagamentoEstornado);
        return ReservaDetalhe.de(salvarEPublicar(reserva));
    }

    @Transactional(readOnly = true)
    public ReservaDetalhe buscar(UUID id) {
        return ReservaDetalhe.de(carregar(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaDetalhe> listar() { return reservaRepository.listar().stream().map(ReservaDetalhe::de).toList(); }

    private Reserva carregar(UUID reservaId) {
        return reservaRepository.buscarPorId(new ReservaId(reservaId))
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));
    }

    private Reserva salvarEPublicar(Reserva reserva) {
        Reserva salva = reservaRepository.salvar(reserva);
        publicadorDeEventos.publicar(reserva.puxarEventos());
        return salva;
    }
}
