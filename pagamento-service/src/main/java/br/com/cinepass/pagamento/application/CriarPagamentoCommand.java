package br.com.cinepass.pagamento.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CriarPagamentoCommand(UUID reservaId, BigDecimal valor, boolean simularRecusa) {}
