package br.com.cinepass.reputacao.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataReputacaoRepository extends JpaRepository<ReputacaoJpaEntity, UUID> {

    /**
     * Garante que a linha do cliente existe, sem nunca sobrescrever uma existente. Duas
     * threads chegando ao mesmo tempo para um cliente novo: uma insere, a outra não faz nada.
     */
    @Modifying
    @Query(value = """
            insert into reputacoes (cliente_id, reservas_confirmadas, reservas_canceladas, pagamentos_recusados,
                                    pontos, valor_total_gasto, nivel, atualizada_em)
            values (:clienteId, 0, 0, 0, 0, 0, 'BRONZE', now())
            on conflict (cliente_id) do nothing
            """, nativeQuery = true)
    void garantirExistencia(@Param("clienteId") UUID clienteId);

    /** {@code select ... for update}: a segunda thread espera a primeira commitar. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReputacaoJpaEntity r where r.clienteId = :clienteId")
    Optional<ReputacaoJpaEntity> bloquear(@Param("clienteId") UUID clienteId);
}
