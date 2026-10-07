package br.com.cinepass.pagamento.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PagamentoTest {
    @Test
    void deveAprovarEEstornarPagamento() {
        var pagamento = Pagamento.solicitar(new ReservaId(UUID.randomUUID()), new Dinheiro(new BigDecimal("50.00")));
        pagamento.aprovar();
        assertEquals(StatusPagamento.APROVADO, pagamento.getStatus());
        pagamento.estornar();
        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
    }
}
