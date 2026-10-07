package br.com.cinepass.ingresso.domain.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IngressoTest {
    @Test
    void deveEmitirECancelar() {
        var ingresso = Ingresso.emitir(new ReservaId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()), List.of("A1"));
        assertEquals(StatusIngresso.EMITIDO, ingresso.getStatus());
        ingresso.cancelar();
        assertEquals(StatusIngresso.CANCELADO, ingresso.getStatus());
    }
}
