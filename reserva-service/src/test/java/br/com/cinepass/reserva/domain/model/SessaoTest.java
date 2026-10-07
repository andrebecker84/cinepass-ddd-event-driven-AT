package br.com.cinepass.reserva.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SessaoTest {
    @Test
    void naoDeveReservarAssentoIndisponivel(){
        var sessao=new Sessao(new SessaoId(UUID.randomUUID()), new FilmeId(UUID.randomUUID()), "1", LocalDateTime.now(),
                new Dinheiro(new BigDecimal("10.00")), Set.of(new AssentoId("A1")));
        sessao.reservar(java.util.List.of(new AssentoId("A1")));
        assertThrows(AssentoIndisponivelException.class, () -> sessao.reservar(java.util.List.of(new AssentoId("A1"))));
    }
}
