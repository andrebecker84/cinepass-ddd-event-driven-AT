package br.com.cinepass.mensageria.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMensagem, Long> {

    /** Pendentes na ordem em que foram gravados. Publicar fora dessa ordem desordenaria a reserva. */
    List<OutboxMensagem> findTop100ByPublicadoEmIsNullOrderByIdAsc();

    Optional<OutboxMensagem> findByEventId(UUID eventId);

    List<OutboxMensagem> findByReservaIdOrderByIdAsc(UUID reservaId);

    long countByPublicadoEmIsNull();

    @Modifying
    @Query("update OutboxMensagem m set m.publicadoEm = :quando, m.tentativas = m.tentativas + 1, m.ultimoErro = null where m.id = :id")
    int marcarPublicada(@Param("id") Long id, @Param("quando") Instant quando);

    @Modifying
    @Query("update OutboxMensagem m set m.tentativas = m.tentativas + 1, m.ultimoErro = :erro where m.id = :id")
    int registrarFalha(@Param("id") Long id, @Param("erro") String erro);

    /** Devolve a linha à fila do relay: a mesma mensagem, com o mesmo eventId, sairá de novo. */
    @Modifying
    @Query("update OutboxMensagem m set m.publicadoEm = null where m.eventId = :eventId")
    int marcarComoPendente(@Param("eventId") UUID eventId);
}
