-- Um banco por microsserviço, no mesmo servidor PostgreSQL. Nenhum serviço conhece as
-- credenciais nem o esquema do banco do outro: o que um sabe do outro chega por evento.
CREATE DATABASE cinepass_db;      -- reserva-service (o nome da base do professor foi mantido)
CREATE DATABASE pagamento_db;     -- pagamento-service
CREATE DATABASE ingresso_db;      -- ingresso-service
CREATE DATABASE notificacao_db;   -- notificacao-service
CREATE DATABASE reputacao_db;     -- reputacao-service
CREATE DATABASE auditoria_db;     -- auditoria-service
