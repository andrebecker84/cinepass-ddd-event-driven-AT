<div align="center">

# 🎯 Assessment (AT): Comunicação orientada a eventos com confiabilidade e observabilidade (DR4-AT)

[![DR4-AT](https://img.shields.io/badge/DR4-Assessment%20(AT)-1E3A8A?style=flat-square)](#-contexto-texto-original)
[![Itens](https://img.shields.io/badge/itens-13-1E3A8A?style=flat-square)](#-os-13-itens-e-onde-cada-um-%C3%A9-respondido)
[![Base](https://img.shields.io/badge/base-leoinfnet%2Fcinepass-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/leoinfnet/cinepass)

[⬅️ README](../../README.md) · [📑 Relatório](../RELATORIO_AT.md) · [📋 Rúbrica](rubrica-DR4-AT.md) · [📨 Eventos](../EVENTOS.md)

</div>

> [!IMPORTANT]
> Transcrição do enunciado fornecido pelo professor. **Esta é a fonte de verdade acadêmica.**
> Em caso de divergência com o runbook, com o README ou com o relatório, prevalece este texto.

---

## 🎬 Escopo adotado: o CinePass no lugar do Freela Marketplace

O enunciado descreve a plataforma **Freela Marketplace**. O professor atualizou a base do AT para o
projeto **CinePass** ([github.com/leoinfnet/cinepass](https://github.com/leoinfnet/cinepass)), e
a orientação é adaptar o enunciado a ele. A tabela abaixo é a tradução usada em todo o trabalho:

| Enunciado (Freela Marketplace) | Este trabalho (CinePass) |
|---|---|
| 🎟️ contrato-service | **reserva-service** |
| 🔄 contrato, ciclo de vida do contrato | **reserva**, ciclo de vida da reserva |
| 🔑 `contratoId` | **`reservaId`** |
| 📨 `ContratoCriado` → `EntregaRegistrada` → `ContratoConcluido` | **`ReservaCriada` → `PagamentoConfirmado` → `ReservaConfirmada`** (e `ReservaCancelada`) |
| 👤 cliente que contrata | cliente que reserva |
| ⭐ freelancer (reputação, histórico) | **cliente** (reputação, histórico) |
| 📥 notificacao-service, reputacao-service, auditoria-service | mesmos nomes |
| 🧭 Eureka, API Gateway, PostgreSQL, Kafka | mesmos componentes |

> [!NOTE]
> A base do professor já tinha `reserva-service`, `pagamento-service`, `ingresso-service`, Eureka e
> API Gateway. Os três serviços auxiliares não existiam e foram criados.

### 📍 Os 13 itens e onde cada um é respondido

| Item | Tema | Relatório |
|:---:|---|---|
| 1 | 📨 Comunicação baseada em eventos | [resposta no relatório](../RELATORIO_AT.md#1--comunica%C3%A7%C3%A3o-baseada-em-eventos) |
| 2 | 🔗 Integração dos serviços | [resposta no relatório](../RELATORIO_AT.md#2--integra%C3%A7%C3%A3o-dos-servi%C3%A7os) |
| 3 | 📜 Especificação das mensagens | [resposta no relatório](../RELATORIO_AT.md#3--especifica%C3%A7%C3%A3o-das-mensagens) |
| 4 | 🔀 Processamento concorrente e ordenação | [resposta no relatório](../RELATORIO_AT.md#4--processamento-concorrente-e-ordena%C3%A7%C3%A3o) |
| 5 | 🔁 Tratamento de mensagens duplicadas | [resposta no relatório](../RELATORIO_AT.md#5--tratamento-de-mensagens-duplicadas) |
| 6 | 📝 Logs da aplicação | [resposta no relatório](../RELATORIO_AT.md#6--logs-da-aplica%C3%A7%C3%A3o) |
| 7 | 🔎 Centralização de logs | [resposta no relatório](../RELATORIO_AT.md#7--centraliza%C3%A7%C3%A3o-de-logs) |
| 8 | 🔭 Rastreamento distribuído | [resposta no relatório](../RELATORIO_AT.md#8--rastreamento-distribu%C3%ADdo) |
| 9 | 🧵 Correlação das operações | [resposta no relatório](../RELATORIO_AT.md#9--correla%C3%A7%C3%A3o-das-opera%C3%A7%C3%B5es) |
| 10 | 🚨 Tratamento de falhas | [resposta no relatório](../RELATORIO_AT.md#10--tratamento-de-falhas) |
| 11 | 🐳 Infraestrutura | [resposta no relatório](../RELATORIO_AT.md#11--infraestrutura) |
| 12 | 📘 Documentação da solução | [resposta no relatório](../RELATORIO_AT.md#12--documenta%C3%A7%C3%A3o-da-solu%C3%A7%C3%A3o) |
| 13 | 📸 Evidências da execução | [resposta no relatório](../RELATORIO_AT.md#13--evid%C3%AAncias-da-execu%C3%A7%C3%A3o) |

---

## 📄 Contexto (texto original)

A plataforma Freela Marketplace conecta clientes que possuem demandas de trabalho a profissionais
freelancers interessados em executar essas atividades.

O sistema possui um serviço responsável pelo ciclo de vida dos contratos e serviços auxiliares
responsáveis por notificações, reputação e auditoria.

A arquitetura atual utiliza microsserviços registrados no Eureka e acessados por meio de um API
Gateway. A infraestrutura disponibiliza PostgreSQL e Apache Kafka.

O objetivo desta entrega é evoluir a comunicação entre os serviços para um modelo orientado a
eventos, adicionando mecanismos de confiabilidade, rastreabilidade e observabilidade das operações
executadas pelo sistema.

## 📌 Escopo da solicitação

A solução deverá permitir que alterações relevantes ocorridas no ciclo de vida de um contrato sejam
comunicadas aos demais serviços da plataforma de maneira assíncrona.

Os serviços interessados deverão reagir aos eventos publicados sem necessidade de chamadas HTTP
diretas entre o serviço de contratos e os serviços consumidores.

A comunicação deverá utilizar o Apache Kafka disponibilizado na infraestrutura do projeto.

---

## 1. 📨 Comunicação baseada em eventos

O contrato-service deverá publicar eventos relacionados às operações relevantes do domínio.

A estrutura das mensagens deverá possuir informações suficientes para identificar:

- o evento;
- o tipo do evento;
- o contrato relacionado;
- a data e hora em que o evento ocorreu;
- os dados necessários para o processamento pelos serviços consumidores;
- informações de correlação utilizadas para rastreamento da operação.

Os eventos deverão ser publicados em tópicos Kafka definidos pela equipe.

A definição dos tópicos, produtores, consumidores, chaves de particionamento e formato das
mensagens deverá ser documentada.

## 2. 🔗 Integração dos serviços

Os serviços auxiliares deverão consumir as mensagens relacionadas às suas responsabilidades.

✉️ **Notificação:** O notificacao-service deverá receber eventos relacionados ao ciclo de vida dos
contratos e registrar as notificações correspondentes. O processamento deverá possuir registros de
log que permitam identificar: o evento recebido; o contrato associado; o destinatário; o resultado
do processamento.

⭐ **Reputação:** O reputacao-service deverá reagir aos eventos que possuam impacto sobre os dados de
reputação ou histórico do freelancer. As alterações realizadas deverão ser persistidas no banco de
dados do próprio serviço.

🗃️ **Auditoria:** O auditoria-service deverá registrar os eventos processados pela plataforma. O
registro deverá permitir consultar posteriormente, no mínimo: identificador do evento; tipo do
evento; contrato relacionado; data e hora; identificador de correlação; conteúdo relevante da
mensagem.

## 3. 📜 Especificação das mensagens

Deverá ser produzida uma especificação dos eventos utilizados na solução.

A documentação deverá apresentar, para cada mensagem: nome do evento; tópico Kafka; serviço
produtor; serviços consumidores; estrutura do payload; campos obrigatórios; chave utilizada para
publicação; exemplo de mensagem.

A documentação deverá representar o contrato de comunicação entre os microsserviços.

## 4. 🔀 Processamento concorrente e ordenação

A solução deverá permitir o processamento concorrente de mensagens referentes a contratos
distintos.

Ao mesmo tempo, deverá ser preservada a ordem dos eventos referentes ao mesmo contrato. Por
exemplo, uma sequência como `ContratoCriado`, `EntregaRegistrada`, `ContratoConcluido` não poderá
ser processada fora de ordem para o mesmo contrato.

A estratégia adotada para particionamento e consumo deverá estar refletida na configuração da
aplicação e na documentação técnica.

## 5. 🔁 Tratamento de mensagens duplicadas

Os consumidores deverão ser capazes de receber novamente uma mensagem já processada sem produzir
efeitos duplicados no sistema.

Cada evento deverá possuir um identificador único.

Os serviços responsáveis por operações persistentes deverão utilizar esse identificador para
reconhecer mensagens anteriormente processadas.

O reprocessamento da mesma mensagem não deverá: duplicar registros; incrementar informações
indevidamente; gerar notificações repetidas; alterar novamente dados que já tenham sido
atualizados pelo mesmo evento.

## 6. 📝 Logs da aplicação

Todos os serviços deverão produzir logs suficientes para acompanhar o processamento das operações.

Os logs deverão conter informações que permitam identificar o contexto de cada execução, incluindo
quando aplicável: nome do serviço; identificador do contrato; identificador do evento;
identificador de correlação; início do processamento; conclusão do processamento; falhas;
mensagens recebidas e publicadas.

Informações sensíveis não deverão ser registradas em log.

## 7. 🔎 Centralização de logs

Os logs produzidos pelos microsserviços deverão ser enviados para uma solução centralizada.

A solução deverá permitir pesquisar eventos e operações executadas em serviços diferentes
utilizando informações em comum, como `contratoId`, `eventId`, `correlationId`.

Deverá ser possível acompanhar uma operação sem precisar consultar separadamente o console de cada
aplicação.

## 8. 🔭 Rastreamento distribuído

As requisições iniciadas através do API Gateway deverão possuir informações de rastreamento
distribuído.

O contexto de tracing deverá ser mantido durante o fluxo da operação, incluindo a comunicação
assíncrona realizada através do Kafka.

Os serviços deverão disponibilizar suas informações de trace para o Zipkin.

Deverá ser possível visualizar no Zipkin a participação dos serviços envolvidos no processamento de
uma operação.

## 9. 🧵 Correlação das operações

Uma operação iniciada externamente deverá possuir uma identificação que possa ser utilizada durante
todo o processamento.

A mesma operação deverá poder ser correlacionada entre: API Gateway → contrato-service → Kafka →
serviços consumidores.

Essa identificação deverá estar disponível nos logs e nas mensagens quando necessário.

## 10. 🚨 Tratamento de falhas

Falhas ocorridas durante o consumo de mensagens deverão ser registradas adequadamente.

Uma falha de processamento não deverá comprometer mensagens não relacionadas.

A solução deverá prever uma estratégia para mensagens que não possam ser processadas após as
tentativas previstas pela aplicação.

As mensagens com erro deverão permanecer disponíveis para diagnóstico e eventual reprocessamento.

## 11. 🐳 Infraestrutura

A infraestrutura disponibilizada no projeto deverá continuar sendo utilizada. A solução possui:
Eureka Server; API Gateway; Apache Kafka em modo KRaft; PostgreSQL; bancos separados por
microsserviço.

Os componentes adicionais necessários para observabilidade poderão ser adicionados ao ambiente
Docker Compose.

A inicialização do ambiente deverá permanecer documentada no projeto.

## 12. 📘 Documentação da solução

A entrega deverá conter documentação suficiente para executar e compreender a solução. A
documentação deverá incluir: visão geral da arquitetura; serviços participantes; tópicos Kafka
utilizados; eventos existentes; formato das mensagens; estratégia de particionamento; estratégia
de idempotência; mecanismo utilizado para publicação transacional; solução adotada para
centralização de logs; configuração utilizada para tracing; instruções para inicialização da
infraestrutura; exemplos de chamadas para geração de eventos.

## 13. 📸 Evidências da execução

A entrega deverá permitir verificar o funcionamento dos fluxos implementados. Deverão ser
apresentadas evidências que demonstrem: requisição recebida pelo API Gateway; alteração persistida
no contrato-service; evento publicado no Kafka; consumo do evento pelos serviços interessados;
persistência realizada pelos consumidores; tratamento de uma mensagem duplicada; manutenção da
ordem dos eventos de um mesmo contrato; logs da mesma operação consultados de forma centralizada;
trace correspondente disponível no Zipkin.

---

## 🏁 Resultado esperado

> [!TIP]
> Ao final da implementação, a plataforma deverá possuir comunicação assíncrona entre os
> microsserviços utilizando Kafka, com eventos documentados, processamento concorrente com
> preservação de ordem por contrato, tratamento de duplicidade, publicação transacional, logs
> centralizados e rastreamento distribuído das operações.

---

<div align="center">

[⬆️ Topo](#-assessment-at-comunica%C3%A7%C3%A3o-orientada-a-eventos-com-confiabilidade-e-observabilidade-dr4-at) · [📑 Relatório](../RELATORIO_AT.md) · [📋 Rúbrica](rubrica-DR4-AT.md)

</div>
