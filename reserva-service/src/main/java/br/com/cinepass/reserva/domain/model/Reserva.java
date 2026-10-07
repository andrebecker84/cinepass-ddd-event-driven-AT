package br.com.cinepass.reserva.domain.model;

import br.com.cinepass.reserva.domain.event.EventoDeReserva;
import br.com.cinepass.reserva.domain.event.PagamentoConfirmado;
import br.com.cinepass.reserva.domain.event.ReservaCancelada;
import br.com.cinepass.reserva.domain.event.ReservaConfirmada;
import br.com.cinepass.reserva.domain.event.ReservaCriada;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root do ciclo de vida da reserva.
 *
 * <p>Cada transição relevante registra um evento de domínio. O agregado não sabe publicar
 * nada: só acumula os fatos, e o serviço de aplicação os entrega ao outbox na mesma
 * transação que grava o novo estado. {@code sequenciaEventos} conta os eventos já emitidos
 * e persiste com a reserva, para que a numeração continue depois de cada recarga.
 */
public class Reserva {
    private final ReservaId id;
    private final ClienteId clienteId;
    private final SessaoId sessaoId;
    private final List<AssentoId> assentos;
    private final Dinheiro valorTotal;
    private StatusReserva status;
    private UUID pagamentoId;
    private UUID ingressoId;
    private MotivoCancelamento motivoCancelamento;
    private long sequenciaEventos;
    private final Instant criadaEm;
    private final List<EventoDeReserva> eventosPendentes = new ArrayList<>();

    private Reserva(ReservaId id, ClienteId clienteId, SessaoId sessaoId, List<AssentoId> assentos, Dinheiro valorTotal,
                    StatusReserva status, UUID pagamentoId, UUID ingressoId, MotivoCancelamento motivoCancelamento,
                    long sequenciaEventos, Instant criadaEm) {
        this.id=Objects.requireNonNull(id); this.clienteId=Objects.requireNonNull(clienteId); this.sessaoId=Objects.requireNonNull(sessaoId);
        this.assentos=List.copyOf(assentos); if (this.assentos.isEmpty()) throw new IllegalArgumentException("A reserva precisa de assentos.");
        this.valorTotal=Objects.requireNonNull(valorTotal); this.status=Objects.requireNonNull(status);
        this.pagamentoId=pagamentoId; this.ingressoId=ingressoId; this.motivoCancelamento=motivoCancelamento;
        this.sequenciaEventos=sequenciaEventos; this.criadaEm=Objects.requireNonNull(criadaEm);
    }

    public static Reserva criar(ClienteId clienteId, SessaoId sessaoId, List<AssentoId> assentos, Dinheiro valorTotal) {
        var reserva = new Reserva(ReservaId.novo(), clienteId, sessaoId, assentos, valorTotal, StatusReserva.CRIADA,
                null, null, null, 0, Instant.now());
        reserva.registrar(new ReservaCriada(UUID.randomUUID(), reserva.id, clienteId, reserva.proximaSequencia(), Instant.now(),
                sessaoId, reserva.assentos, valorTotal));
        return reserva;
    }

    public static Reserva restaurar(ReservaId id, ClienteId clienteId, SessaoId sessaoId, List<AssentoId> assentos, Dinheiro valorTotal,
                                    StatusReserva status, UUID pagamentoId, UUID ingressoId, MotivoCancelamento motivoCancelamento,
                                    long sequenciaEventos, Instant criadaEm) {
        return new Reserva(id, clienteId, sessaoId, assentos, valorTotal, status, pagamentoId, ingressoId, motivoCancelamento,
                sequenciaEventos, criadaEm);
    }

    public void aguardarPagamento() { exigir(StatusReserva.CRIADA); status = StatusReserva.AGUARDANDO_PAGAMENTO; }

    public void confirmarPagamento(UUID pagamentoId) {
        exigir(StatusReserva.AGUARDANDO_PAGAMENTO); this.pagamentoId = Objects.requireNonNull(pagamentoId); status = StatusReserva.PAGAMENTO_APROVADO;
        registrar(new PagamentoConfirmado(UUID.randomUUID(), id, clienteId, proximaSequencia(), Instant.now(), pagamentoId, valorTotal));
    }

    public void iniciarEmissaoIngresso() { exigir(StatusReserva.PAGAMENTO_APROVADO); status = StatusReserva.EMITINDO_INGRESSO; }

    public void confirmar(UUID ingressoId) {
        exigir(StatusReserva.EMITINDO_INGRESSO); this.ingressoId = Objects.requireNonNull(ingressoId); status = StatusReserva.CONFIRMADA;
        registrar(new ReservaConfirmada(UUID.randomUUID(), id, clienteId, proximaSequencia(), Instant.now(),
                sessaoId, assentos, valorTotal, pagamentoId, ingressoId));
    }

    /**
     * Encerra a reserva sem conclusão. Idempotente: cancelar de novo não muda nada e não
     * emite um segundo evento. Uma reserva confirmada não pode ser cancelada por este caminho.
     */
    public void cancelar(MotivoCancelamento motivo, UUID pagamentoId, boolean pagamentoEstornado) {
        if (status == StatusReserva.CANCELADA) return;
        if (status == StatusReserva.CONFIRMADA) throw new IllegalStateException("Uma reserva confirmada não pode ser cancelada pela saga.");
        this.motivoCancelamento = Objects.requireNonNull(motivo);
        if (pagamentoId != null) this.pagamentoId = pagamentoId;
        status = StatusReserva.CANCELADA;
        registrar(new ReservaCancelada(UUID.randomUUID(), id, clienteId, proximaSequencia(), Instant.now(),
                motivo, valorTotal, this.pagamentoId, pagamentoEstornado));
    }

    /** Entrega os eventos acumulados e esvazia a lista: cada evento sai uma única vez. */
    public List<EventoDeReserva> puxarEventos() {
        var eventos = List.copyOf(eventosPendentes);
        eventosPendentes.clear();
        return eventos;
    }

    private void registrar(EventoDeReserva evento) { eventosPendentes.add(evento); }
    private long proximaSequencia() { return ++sequenciaEventos; }
    private void exigir(StatusReserva esperado) { if (status != esperado) throw new IllegalStateException("Estado inválido. Esperado: " + esperado + ", atual: " + status); }

    public ReservaId getId(){return id;} public ClienteId getClienteId(){return clienteId;} public SessaoId getSessaoId(){return sessaoId;}
    public List<AssentoId> getAssentos(){return assentos;} public Dinheiro getValorTotal(){return valorTotal;} public StatusReserva getStatus(){return status;}
    public UUID getPagamentoId(){return pagamentoId;} public UUID getIngressoId(){return ingressoId;} public Instant getCriadaEm(){return criadaEm;}
    public MotivoCancelamento getMotivoCancelamento(){return motivoCancelamento;} public long getSequenciaEventos(){return sequenciaEventos;}
}
