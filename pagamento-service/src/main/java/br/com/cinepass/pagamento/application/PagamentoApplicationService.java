package br.com.cinepass.pagamento.application;

import br.com.cinepass.pagamento.domain.model.Dinheiro;
import br.com.cinepass.pagamento.domain.model.Pagamento;
import br.com.cinepass.pagamento.domain.model.PagamentoId;
import br.com.cinepass.pagamento.domain.model.ReservaId;
import br.com.cinepass.pagamento.domain.repository.PagamentoRepository;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PagamentoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(PagamentoApplicationService.class);
    private final PagamentoRepository repository;

    public PagamentoApplicationService(PagamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pagamento processar(CriarPagamentoCommand command) {
        Pagamento pagamento = Pagamento.solicitar(
                new ReservaId(command.reservaId()),
                new Dinheiro(command.valor())
        );

        if (command.simularRecusa()) {
            pagamento.recusar();
        } else {
            pagamento.aprovar();
        }

        Pagamento salvo = repository.salvar(pagamento);
        try (var mdc = MdcDoEvento.de(command.reservaId(), null, null, null)) {
            log.info("Cobrança processada: pagamento={} valor={} resultado={}", salvo.getId().valor(), salvo.getValor().valor(), salvo.getStatus());
        }
        return salvo;
    }

    @Transactional
    public Pagamento estornar(UUID pagamentoId) {
        Pagamento pagamento = repository.buscarPorId(new PagamentoId(pagamentoId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
        pagamento.estornar();
        Pagamento salvo = repository.salvar(pagamento);
        try (var mdc = MdcDoEvento.de(salvo.getReservaId().valor(), null, null, null)) {
            log.info("Estorno processado (compensação da Saga): pagamento={} resultado={}", pagamentoId, salvo.getStatus());
        }
        return salvo;
    }

    @Transactional(readOnly = true)
    public Pagamento buscar(UUID pagamentoId) {
        return repository.buscarPorId(new PagamentoId(pagamentoId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorReserva(UUID reservaId) {
        return repository.buscarPorReservaId(new ReservaId(reservaId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(reservaId));
    }
}
