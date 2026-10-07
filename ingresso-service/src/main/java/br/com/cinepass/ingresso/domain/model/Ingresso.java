package br.com.cinepass.ingresso.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Ingresso {
    private final IngressoId id;
    private final ReservaId reservaId;
    private final SessaoId sessaoId;
    private final List<String> assentos;
    private final CodigoIngresso codigo;
    private StatusIngresso status;
    private final Instant emitidoEm;

    private Ingresso(IngressoId id, ReservaId reservaId, SessaoId sessaoId, List<String> assentos,
                     CodigoIngresso codigo, StatusIngresso status, Instant emitidoEm) {
        this.id = Objects.requireNonNull(id);
        this.reservaId = Objects.requireNonNull(reservaId);
        this.sessaoId = Objects.requireNonNull(sessaoId);
        this.assentos = List.copyOf(assentos);
        if (this.assentos.isEmpty()) throw new IllegalArgumentException("O ingresso precisa ter ao menos um assento.");
        this.codigo = Objects.requireNonNull(codigo);
        this.status = Objects.requireNonNull(status);
        this.emitidoEm = Objects.requireNonNull(emitidoEm);
    }

    public static Ingresso emitir(ReservaId reservaId, SessaoId sessaoId, List<String> assentos) {
        return new Ingresso(IngressoId.novo(), reservaId, sessaoId, assentos, CodigoIngresso.gerar(), StatusIngresso.EMITIDO, Instant.now());
    }

    public static Ingresso restaurar(IngressoId id, ReservaId reservaId, SessaoId sessaoId, List<String> assentos,
                                     CodigoIngresso codigo, StatusIngresso status, Instant emitidoEm) {
        return new Ingresso(id, reservaId, sessaoId, assentos, codigo, status, emitidoEm);
    }

    public void cancelar() {
        if (status == StatusIngresso.CANCELADO) return;
        status = StatusIngresso.CANCELADO;
    }

    public IngressoId getId() { return id; }
    public ReservaId getReservaId() { return reservaId; }
    public SessaoId getSessaoId() { return sessaoId; }
    public List<String> getAssentos() { return assentos; }
    public CodigoIngresso getCodigo() { return codigo; }
    public StatusIngresso getStatus() { return status; }
    public Instant getEmitidoEm() { return emitidoEm; }
}
