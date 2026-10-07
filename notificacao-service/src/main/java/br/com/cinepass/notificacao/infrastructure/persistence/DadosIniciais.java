package br.com.cinepass.notificacao.infrastructure.persistence;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Contatos fictícios dos clientes de demonstração. O cliente
 * {@code 44444444-4444-4444-4444-444444444444} fica de fora de propósito: as notificações
 * dele falham, esgotam as tentativas e vão para a DLT, de onde podem ser reprocessadas
 * depois que o contato for cadastrado.
 */
@Component
class DadosIniciais implements CommandLineRunner {
    static final UUID CLIENTE_MARIA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID CLIENTE_JOAO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final SpringDataDestinatarioRepository destinatarios;

    DadosIniciais(SpringDataDestinatarioRepository destinatarios) { this.destinatarios = destinatarios; }

    @Override @Transactional
    public void run(String... args) {
        if (!destinatarios.existsById(CLIENTE_MARIA)) destinatarios.save(new DestinatarioJpaEntity(CLIENTE_MARIA, "Maria Silva", "maria.silva@example.com"));
        if (!destinatarios.existsById(CLIENTE_JOAO)) destinatarios.save(new DestinatarioJpaEntity(CLIENTE_JOAO, "João Souza", "joao.souza@example.com"));
    }
}
