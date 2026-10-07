<div align="center">

# 📋 Rúbrica: DR4-AT

[![Competências](https://img.shields.io/badge/compet%C3%AAncias-4-1E3A8A?style=flat-square)](#-1-transformar-mon%C3%B3litos-em-microsservi%C3%A7os-eficazes-aplicando-princ%C3%ADpios-de-ddd-e-t%C3%A9cnicas-de-decomposi%C3%A7%C3%A3o)
[![Critérios](https://img.shields.io/badge/crit%C3%A9rios-16-1E3A8A?style=flat-square)](#-1-transformar-mon%C3%B3litos-em-microsservi%C3%A7os-eficazes-aplicando-princ%C3%ADpios-de-ddd-e-t%C3%A9cnicas-de-decomposi%C3%A7%C3%A3o)
[![Evidências](https://img.shields.io/badge/evid%C3%AAncias-19-DC2626?style=flat-square)](../evidencias)

[⬅️ README](../../README.md) · [📑 Relatório](../RELATORIO_AT.md) · [🎯 Enunciado](enunciado-DR4-AT.md) · [📨 Eventos](../EVENTOS.md)

</div>

> [!NOTE]
> Transcrição da rúbrica fornecida pelo professor, com a numeração usada neste trabalho e o
> mapeamento de cada critério para o código e para as evidências. Onde a rúbrica diz
> "contrato-service", leia **reserva-service** (ver [enunciado](enunciado-DR4-AT.md)).

---

## 🧱 1. Transformar monólitos em microsserviços eficazes, aplicando princípios de DDD e técnicas de decomposição

| # | Critério (texto do professor) | Onde está |
|:---:|---|---|
| 1.1 | O aluno decompôs a plataforma em microsserviços autônomos integrados via API Gateway e Eureka Server, mantendo bancos de dados PostgreSQL isolados por serviço? | `docker-compose.yml`, `infra/postgres/init.sql` (6 bancos); evidências [01](../evidencias/01-docker-compose-ps.png), [02](../evidencias/02-eureka.png) |
| 1.2 | O aluno garantiu a independência do ciclo de vida do contrato no contrato-service sem chamadas HTTP síncronas diretas para os serviços auxiliares? | `SagaDeReserva`, `PublicadorDeEventosOutbox`; nenhum cliente HTTP para notificação, reputação ou auditoria |
| 1.3 | O aluno documentou a visão geral da arquitetura, a lista de serviços e as instruções completas para a inicialização do ambiente Docker Compose? | [`README.md`](../../README.md), relatório [§11](../RELATORIO_AT.md#11--infraestrutura) e [§12](../RELATORIO_AT.md#12--documenta%C3%A7%C3%A3o-da-solu%C3%A7%C3%A3o) |
| 1.4 | O aluno apresentou evidências completas da execução do fluxo, desde a requisição no Gateway até a alteração persistida no contrato-service? | evidências [03](../evidencias/03-gateway-requisicao.png), [04](../evidencias/04-reserva-persistida-outbox.png) |

## 📣 2. Projetar softwares usando "domain events"

| # | Critério | Onde está |
|:---:|---|---|
| 2.1 | O aluno definiu e publicou eventos de domínio no Apache Kafka contendo identificador do evento, tipo, contrato associado, data/hora e dados do payload? | `domain/event/*`, `EventoEnvelope`; evidências [05](../evidencias/05-kafka-topico.png), [06](../evidencias/06-kafka-envelope.png) |
| 2.2 | O aluno elaborou a documentação de especificação das mensagens contendo tópicos, produtor, consumidores, chave de publicação, campos obrigatórios e exemplo de payload? | [`docs/EVENTOS.md`](../EVENTOS.md), [`docs/asyncapi.yaml`](../asyncapi.yaml) |
| 2.3 | O aluno implementou a estratégia de publicação transacional para assegurar a consistência no envio de eventos a partir das alterações do modelo de domínio? | `PublicadorOutbox`, `RelayOutbox`, `OutboxTransacionalTest`; evidência [04](../evidencias/04-reserva-persistida-outbox.png) |
| 2.4 | O aluno configurou os serviços de Notificação, Reputação e Auditoria para reagirem de forma reativa e assíncrona aos eventos publicados no Kafka? | `OuvinteDeEventosDeReserva` (×3); evidências [08](../evidencias/08-kafka-consumidores.png), [09](../evidencias/09-persistencia-consumidores.png) |

## ⚡ 3. Desenvolver microsserviços event-driven e com outros padrões de comunicação assíncrona

| # | Critério | Onde está |
|:---:|---|---|
| 3.1 | O aluno estruturou o particionamento e o consumo concorrente no Kafka utilizando a chave do contrato para preservar a ordem sequencial dos eventos de uma mesma entidade? | chave = `reservaId`, 3 partições, `concurrency: 3`; `OrdenacaoEConcorrenciaTest`; evidências [11](../evidencias/11-ordem-concorrencia.png), [12](../evidencias/12-kibana-threads.png) |
| 3.2 | O aluno implementou o tratamento de idempotência nos consumidores utilizando o identificador único do evento para impedir duplicidade de registros, contadores e notificações? | `ConsumidorIdempotente`, `EventoProcessado`; testes de duplicidade; evidência [10](../evidencias/10-mensagem-duplicada.png) |
| 3.3 | O aluno desenvolveu a estratégia de tratamento de falhas e reprocessamento para mensagens com erro, mantendo-as disponíveis em tópicos ou filas de tratamento para diagnóstico? | `InboxAutoConfiguration` (retentativa + DLT), `ReprocessadorDeDlt`; evidências [16](../evidencias/16-dlt-falha.png), [17](../evidencias/17-kafka-dlt.png), [18](../evidencias/18-dlt-reprocessamento.png) |
| 3.4 | O aluno apresentou evidências da manutenção da ordem dos eventos de um mesmo contrato e do reprocessamento de mensagens duplicadas sem causar efeitos colaterais? | evidências [10](../evidencias/10-mensagem-duplicada.png), [11](../evidencias/11-ordem-concorrencia.png), [12](../evidencias/12-kibana-threads.png) |

## 🔭 4. Implementar testes e observabilidade em microsserviços com Zipkin, Spring Cloud Sleuth e ELK Stack

| # | Critério | Onde está |
|:---:|---|---|
| 4.1 | O aluno propagou o identificador de correlação (correlationId) a partir do API Gateway através do contrato-service, Apache Kafka e serviços consumidores? | `CorrelacaoGlobalFilter`, `CorrelacaoFilter`, envelope e cabeçalho Kafka; evidências [03](../evidencias/03-gateway-requisicao.png), [07](../evidencias/07-kafka-cabecalhos.png), [13](../evidencias/13-kibana-correlacao.png) |
| 4.2 | O aluno padronizou os logs das aplicações contendo nome do serviço, contratoId, eventId e correlationId, garantindo a ausência de dados sensíveis? | `cinepass-logback.xml`, `MdcDoEvento`, `Email.mascarado()`; evidências [10](../evidencias/10-mensagem-duplicada.png), [13](../evidencias/13-kibana-correlacao.png), [16](../evidencias/16-dlt-falha.png) |
| 4.3 | O aluno integrou os microsserviços a uma solução de centralização de logs que permite a consulta e filtragem unificada de operações por correlationId e contratoId? | ELK 9.5 (Logstash TCP → Elasticsearch → Kibana); evidências [12](../evidencias/12-kibana-threads.png), [13](../evidencias/13-kibana-correlacao.png) |
| 4.4 | O aluno configurou o rastreamento distribuído nos microsserviços e no Apache Kafka exportando as informações para o Zipkin e demonstrando o trace completo no painel? | Micrometer Tracing + Brave + Zipkin; `ContextoDeRastreamento`; evidência [14](../evidencias/14-zipkin-trace.png) |

---

## 🔍 Leitura dos critérios

> [!NOTE]
> **"Spring Cloud Sleuth" no título da competência 4.** O Sleuth foi descontinuado: a última linha
> é a 3.1, para Spring Boot 2.x. No Spring Boot 3 e 4 o mesmo papel é do **Micrometer Tracing**, com
> a ponte para o Brave (a biblioteca que o Sleuth usava por baixo) e o exportador para o Zipkin. É a
> substituição oficial indicada pela própria equipe do Spring, e está explicada no relatório.

> [!TIP]
> **Java 25.** A base do professor está em Java 21. O aluno pediu as versões mais atuais e estáveis;
> o Java 25 é LTS e é compatível com Spring Boot 4.1 e Spring Cloud 2025.1.

---

<div align="center">

[⬆️ Topo](#-rúbrica-dr4-at) · [📑 Relatório](../RELATORIO_AT.md) · [🎯 Enunciado](enunciado-DR4-AT.md)

</div>
