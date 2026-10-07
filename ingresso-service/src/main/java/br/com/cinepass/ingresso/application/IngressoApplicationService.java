package br.com.cinepass.ingresso.application;

import br.com.cinepass.ingresso.domain.model.*;
import br.com.cinepass.ingresso.domain.repository.IngressoRepository;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngressoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(IngressoApplicationService.class);
    private final IngressoRepository repository;

    public IngressoApplicationService(IngressoRepository repository) { this.repository = repository; }

    @Transactional
    public Ingresso emitir(EmitirIngressoCommand command) {
        try (var mdc = MdcDoEvento.de(command.reservaId(), null, null, null)) {
            if (command.simularFalha()) {
                log.error("Emissão do ingresso falhou (falha simulada) para os assentos {}", command.assentos());
                throw new EmissaoIngressoException("Falha proposital na emissão do ingresso para demonstrar a compensação da Saga.");
            }
            Ingresso ingresso = repository.salvar(Ingresso.emitir(new ReservaId(command.reservaId()), new SessaoId(command.sessaoId()), command.assentos()));
            log.info("Ingresso emitido: codigo={} assentos={}", ingresso.getCodigo().valor(), ingresso.getAssentos());
            return ingresso;
        }
    }

    @Transactional
    public Ingresso cancelar(UUID ingressoId) {
        Ingresso ingresso = repository.buscarPorId(new IngressoId(ingressoId))
                .orElseThrow(() -> new IngressoNaoEncontradoException(ingressoId));
        ingresso.cancelar();
        return repository.salvar(ingresso);
    }

    @Transactional(readOnly = true)
    public Ingresso buscarPorReserva(UUID reservaId) {
        return repository.buscarPorReserva(new ReservaId(reservaId))
                .orElseThrow(() -> new IngressoNaoEncontradoException(reservaId));
    }
}
