# Feature Specification: Calendário completo e acompanhamentos personalizados

**Feature Branch**: existing working branch; no branch creation requested
**Created**: 2026-11-07
**Status**: Implemented 2026-11-07; evidence and limits in verification.md.
**Input**: `TODO/specify_calendario_acompanhamentos_app.md` (body after `---`); decisions in
`TODO/planejamento_calendario_acompanhamentos.md`. Origin conversation: Codex session
`01a1b12c-0445-7cc0-a8a0-d3ff1377af37` (not consulted here; the prompt records every decision).
The current user authorized specify → plan → tasks → implement in this chat.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Montar o formulário de acompanhamento de um hábito (Priority: P1)

Como usuário, crio para um hábito (positivo ou negativo) um formulário próprio com perguntas de escala
numérica, escolha única, escolha múltipla e texto livre, marco cada uma como obrigatória ou opcional, escolho
uma escala principal, defino a ordem e escolho entre um registro por dia (padrão) ou vários por dia.
**Why this priority**: sem formulário não há acompanhamento; é a base de todas as outras histórias.
**Independent Test**: criar "Intensidade da vontade" (escala 0–10) e "Quais gatilhos você percebeu?"
(múltipla), reordenar, reabrir o app e conferir a estrutura e a ordem.

**Acceptance Scenarios**:
1. **Given** um hábito sem acompanhamento, **When** abro seus detalhes, **Then** posso criar um acompanhamento;
   hábitos sem acompanhamento continuam idênticos ao comportamento atual.
2. **Given** um formulário novo, **When** adiciono uma escala, **Then** ela começa com faixa 0–10 e posso definir
   nome, mínimo, máximo e descrições dos extremos; mínimo ≥ máximo é rejeitado.
3. **Given** perguntas dos quatro tipos, **When** marco obrigatória/opcional e salvo, **Then** as marcações e os
   tipos são preservados após reabrir o app.
4. **Given** uma escala marcada como principal na terceira posição, **When** salvo, **Then** ela continua na
   terceira posição; marcar outra escala como principal desmarca a anterior (no máximo uma principal).
5. **Given** perguntas reordenadas, **When** reabro o formulário, **Then** a ordem escolhida é mantida.
6. **Given** uma pergunta de escolha, **When** crio, renomeio e reordeno opções (com descrição curta opcional) e
   escolho resposta única ou múltipla, **Then** novos preenchimentos usam exatamente essa configuração.
7. **Given** um formulário salvo, **When** escolho "vários por dia", **Then** o modo é preservado; o padrão é
   "um por dia".
8. **Given** um formulário sem perguntas ou uma escolha sem opções, **When** tento salvar, **Then** o app explica
   o problema e não salva.

---

### User Story 2 - Preencher, editar e excluir registros com data escolhida (Priority: P1)

Como usuário, preencho o acompanhamento para hoje ou para uma data passada desde a criação do acompanhamento,
edito um registro existente e excluo um registro individual com confirmação. O app guarda a data de referência
e o momento real em que preenchi.
**Why this priority**: o valor está no registro de contexto ao longo do tempo.
**Independent Test**: preencher ontem às 9h de hoje; tentar amanhã; preencher hoje duas vezes no modo um por dia.

**Acceptance Scenarios**:
1. **Given** um acompanhamento criado anteontem, **When** preencho com data de ontem, **Then** o registro tem
   data de referência ontem e momento de preenchimento agora, exibidos separadamente.
2. **Given** uma data futura ou anterior à criação do acompanhamento, **When** tento preencher, **Then** é
   rejeitada com explicação.
3. **Given** o modo um por dia e uma data já preenchida, **When** escolho essa data, **Then** abre a edição do
   registro existente; nunca há dois registros para a mesma data.
4. **Given** o modo vários por dia, **When** preencho três vezes no mesmo dia, **Then** três registros
   individuais são mantidos e exibidos separadamente.
5. **Given** uma pergunta obrigatória sem resposta, **When** salvo, **Then** o app aponta a pergunta e não salva;
   perguntas opcionais podem ficar em branco.
6. **Given** um registro, **When** peço para excluir e confirmo, **Then** apenas esse registro some; cancelar não
   muda nada.
7. **Given** qualquer ação de acompanhamento (criar, editar formulário, preencher, editar, excluir), **When**
   concluída, **Then** saldo de moedas, XP, metas, sequências, revisão, recompensas e registros do hábito
   permanecem inalterados; um valor alto de vontade não é tratado como falha nem ocorrência.

---

### User Story 3 - Versões do formulário preservam o histórico (Priority: P1)

Como usuário, altero o formulário ao longo do tempo sem perder o significado de registros antigos.
**Why this priority**: sem versionamento a edição do formulário corromperia o histórico.
**Independent Test**: preencher, remover uma opção, adicionar uma pergunta obrigatória, renomear a escala e
editar o registro antigo.

**Acceptance Scenarios**:
1. **Given** registros feitos com a versão 1, **When** salvo alterações no formulário, **Then** novos registros
   usam a versão nova e os antigos continuam com perguntas, opções, escalas e rótulos da versão 1.
2. **Given** uma opção removida, **When** preencho de novo, **Then** ela não aparece; registros antigos que a
   usaram ainda mostram seu rótulo histórico.
3. **Given** uma pergunta obrigatória adicionada depois, **When** edito um registro antigo, **Then** o app usa a
   versão original e não exige a nova pergunta.
4. **Given** perguntas reordenadas na versão atual, **When** abro um registro antigo, **Then** ele mantém a ordem
   da versão com que foi respondido.

---

### User Story 4 - Calendário mensal, semanal e diário com filtros (Priority: P2)

Como usuário, abro a aba Calendário e navego por mês, semana ou dia, filtrando por lista, por hábito e por tipo
(tarefas, hábitos, acompanhamentos). Ao tocar uma data vejo seus itens e registros e posso preencher ou editar
o acompanhamento daquele dia.
**Why this priority**: substitui a busca por texto atual e reúne tudo por data; depende de US1/US2 apenas para
o tipo acompanhamentos.
**Independent Test**: com tarefas, registros de hábito e acompanhamentos em datas variadas, alternar visões,
aplicar filtros e abrir um dia.

**Acceptance Scenarios**:
1. **Given** a aba Calendário, **When** abro, **Then** vejo a visão mensal do mês atual com navegação para mês
   anterior/seguinte e opções de visão semanal (segunda a domingo) e diária; não há mais campo de data digitada.
2. **Given** tarefas abertas com prazo em uma data, **When** abro essa data, **Then** as mesmas tarefas que a
   busca anterior exibia para a data aparecem e abrem seus detalhes existentes.
3. **Given** registros de hábito, **When** abro uma data, **Then** vejo os registros existentes daquela data real;
   o calendário não cria novas regras de agendamento de hábitos.
4. **Given** dias com registros de acompanhamento, **When** vejo o mês ou a semana, **Then** esses dias têm um
   indicador discreto; dias sem registro mostram "Sem dados" no dia aberto, nunca zero, abstinência ou falha.
5. **Given** filtros de tipo, lista e hábito, **When** combino filtros, **Then** apenas os itens que atendem a
   todos aparecem e os indicadores seguem os filtros.
6. **Given** um dia aberto dentro dos limites de um acompanhamento, **When** toco preencher, **Then** abre o
   formulário com essa data; no modo um por dia com registro existente abre a edição; datas fora do limite não
   oferecem preencher.
7. **Given** cada célula do calendário, **When** uso TalkBack, **Then** a célula anuncia data e quantidade de
   itens/registros visíveis.

---

### User Story 5 - Análise simples por acompanhamento (Priority: P3)

Como usuário, seleciono um acompanhamento e vejo a evolução da escala principal e a frequência das opções
escolhidas em 7, 14, 30 ou 90 dias.
**Why this priority**: valor de reflexão; depende de registros existentes.
**Independent Test**: com registros espaçados e um dia com vários registros, alternar períodos.

**Acceptance Scenarios**:
1. **Given** registros com escala principal, **When** escolho 7/14/30/90 dias, **Then** vejo os valores do
   período em ordem de data, cada registro como ponto individual (sem soma nem média diária).
2. **Given** dias sem registro no período, **When** vejo a evolução, **Then** aparecem como lacunas "Sem dados",
   não como zero.
3. **Given** perguntas de escolha, **When** vejo a análise, **Then** vejo quantas vezes cada opção foi escolhida
   no período, incluindo opções removidas com rótulo histórico.
4. **Given** a faixa da escala principal alterada entre versões, **When** vejo a evolução, **Then** cada valor é
   mostrado com sua própria faixa e o app avisa que faixas diferentes não são diretamente comparáveis; valores
   antigos não são reescalados.
5. **Given** dois acompanhamentos, **When** vejo a análise, **Then** nunca há mistura de escalas de
   acompanhamentos diferentes e nenhum texto sugere diagnóstico ou causalidade.

---

### User Story 6 - Lembrete persistente para preencher (Priority: P3)

Como usuário, defino um horário diário para lembrar de preencher o acompanhamento. Recebo um aviso neutro que
continua visível até eu salvar um registro do dia, com ações Preencher e Adiar.
**Why this priority**: aumenta a constância; reutiliza a infraestrutura de lembretes existente.
**Independent Test**: lembrete às 21:00, dispensar o aviso, conferir pendência no app, preencher e ver o aviso
desaparecer.

**Acceptance Scenarios**:
1. **Given** um lembrete às 21:00 e nenhum registro hoje, **When** chega o horário, **Then** aparece "Você tem um
   acompanhamento para preencher", sem nome do hábito, perguntas ou respostas no aviso, na tela bloqueada ou no
   nome do canal.
2. **Given** o aviso, **When** toco Preencher, **Then** o app existente abre o formulário para a data pendente;
   abrir sem salvar não resolve a pendência.
3. **Given** o aviso, **When** toco Adiar, **Then** uso as opções de adiamento existentes e o mesmo aviso volta no
   novo horário sem duplicar.
4. **Given** um registro válido salvo para a data pendente, **When** salvo, **Then** o aviso é removido e a
   pendência some.
5. **Given** o aviso dispensado pelo usuário (permitido em Android 14+ mesmo para avisos contínuos), **When**
   abro o app, **Then** a pendência continua visível no hábito e no Hoje; o app não promete aviso impossível de
   dispensar nem usa serviço em primeiro plano para mantê-lo.
6. **Given** notificações negadas ou canal desativado, **When** configuro o lembrete, **Then** o app explica a
   limitação usando o fluxo de permissão existente e a pendência continua visível no app.
7. **Given** o hábito pausado/excluído ou o acompanhamento removido, **When** o horário chega, **Then** não há
   aviso e avisos já exibidos são removidos.
8. **Given** o aparelho desligado durante vários dias com lembrete, **When** religo, **Then** no máximo um aviso
   de acompanhamento (para hoje, se o horário já passou) é exibido; não há rajada de pendências passadas.

### Edge Cases

- Hábito criado hoje com acompanhamento criado hoje: apenas hoje é preenchível.
- Mudança de "vários por dia" para "um por dia" com dias que já têm vários registros: registros existentes são
  preservados e mostrados; a data abre o registro mais recente para edição e nenhum novo é criado nela.
- Remoção da escala principal ou de todas as escalas: a análise da escala fica indisponível com explicação; a
  frequência de opções continua.
- Opção renomeada: registros antigos mantêm o rótulo da versão em que foram respondidos.
- Hábito pausado: o acompanhamento continua consultável e editável para datas passadas permitidas; não há
  lembrete enquanto pausado. Hábito excluído: histórico preservado como o do hábito, somente leitura, sem novos
  registros nem lembretes.
- Virada de dia ou mudança de fuso: "hoje" segue a data civil do aparelho no momento da ação.
- Fonte ampliada: controles de escala usam botões de valor anunciáveis, não apenas arrasto.
- Falha de leitura/gravação: estado de erro recuperável com repetir; nenhum registro parcial é salvo.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

Tudo é local e offline; sem login, backend, telemetria ou sincronização. Incluído: construtor de
acompanhamento por hábito, versões do formulário, registros datados, calendário mês/semana/dia com filtros,
análise simples por acompanhamento e lembrete diário neutro. Excluído desta entrega: PIN/biometria, exportação,
criptografia específica, fluxos de apagar tudo, módulos clínicos, formulário fixo de compulsões ou exemplos
íntimos pré-preenchidos, perfil derivado de respostas, metas/indicadores calculados das respostas, intervalos
entre episódios, duração de comportamento dentro do acompanhamento, sincronização e análise remota. A
configuração atual de backup/extração de dados não muda (aberto: decidir se respostas devem ser excluídas de
backups futuros). Permissões: apenas a de notificação já existente, pedida em contexto ao configurar o lembrete.
Dados pessoais (nomes, perguntas, respostas) nunca entram em logs nem no conteúdo do aviso. Interface em
português, temas claro/escuro/sistema, direção visual B, descrições acessíveis e estados de carregamento, vazio,
conteúdo e erro. Datas usam calendário civil do fuso do aparelho; semanas segunda–domingo.

### Functional Requirements

- **FR-001**: Permitir criar, editar e remover um acompanhamento opcional por hábito; hábitos sem
  acompanhamento mantêm o comportamento atual.
- **FR-002**: Oferecer perguntas de escala numérica, escolha única, escolha múltipla e texto livre, cada uma
  marcada como obrigatória ou opcional.
- **FR-003**: Configurar escala com nome, mínimo, máximo e descrições dos extremos; padrão 0–10; mínimo < máximo.
- **FR-004**: Permitir no máximo uma escala principal, usada só para destaque e análise, sem alterar a posição.
- **FR-005**: Permitir reordenar perguntas e preservar a ordem escolhida.
- **FR-006**: Permitir criar, renomear, reordenar e remover opções de escolha (com descrição curta opcional) e
  definir resposta única ou múltipla por pergunta.
- **FR-007**: Salvar cada alteração do formulário como nova versão; registros guardam a versão respondida e são
  exibidos/editados com perguntas, opções, escalas, rótulos e ordem dessa versão.
- **FR-008**: Opções removidas somem de novos preenchimentos e permanecem com rótulo histórico nos registros.
- **FR-009**: Oferecer modo um registro por dia (padrão) ou vários por dia.
- **FR-010**: Guardar data de referência escolhida e momento real do preenchimento; aceitar datas da criação do
  acompanhamento até hoje e rejeitar futuras ou anteriores.
- **FR-011**: No modo um por dia, abrir a edição do registro existente da data em vez de criar duplicata,
  garantido também no armazenamento.
- **FR-012**: Validar respostas obrigatórias, valores dentro da faixa e opções existentes na versão.
- **FR-013**: Permitir excluir um registro individual com confirmação; não oferecer exclusão em massa.
- **FR-014**: Nenhuma ação de acompanhamento altera moedas, XP, metas, sequências, revisão, recompensas ou
  registros do hábito.
- **FR-015**: Seguir o ciclo de vida do hábito: pausado mantém consulta/edição de datas passadas permitidas e
  suspende lembretes; excluído preserva histórico somente leitura.
- **FR-016**: Substituir a busca por data digitada do Calendário por visões mensal, semanal e diária navegáveis.
- **FR-017**: Exibir por data as tarefas que a busca atual exibia, registros de hábito pela data real e
  registros de acompanhamento, abrindo detalhes/formulários existentes.
- **FR-018**: Filtrar por tipo (tarefas, hábitos, acompanhamentos), por lista e por hábito, combinados.
- **FR-019**: Indicar discretamente dias com registros de acompanhamento e mostrar "Sem dados" quando não houver.
- **FR-020**: Permitir preencher ou editar o acompanhamento a partir de um dia, respeitando modo e limites.
- **FR-021**: Mostrar, por acompanhamento, evolução da escala principal e frequência de opções em 7/14/30/90 dias.
- **FR-022**: Não misturar acompanhamentos, não somar nem tirar média de registros do mesmo dia, mostrar dias
  sem registro como lacunas e não usar linguagem de diagnóstico ou causalidade.
- **FR-023**: Não reescalar valores de faixas diferentes; mostrar cada valor com sua faixa e avisar quando a
  faixa variar no período.
- **FR-024**: Permitir lembrete diário em horário escolhido estendendo a infraestrutura de lembretes existente
  (canais, permissão, agendamento, adiar, precisão aproximada, recuperação sem rajada, abertura da Activity).
- **FR-025**: Aviso de acompanhamento com texto neutro, contínuo quando o sistema permitir, sem nome/perguntas/
  respostas, com Preencher e Adiar; dispensar não resolve a pendência; só salvar um registro válido da data
  pendente resolve.
- **FR-026**: Parar e remover avisos quando o hábito for pausado/excluído ou o acompanhamento removido.
- **FR-027**: Mostrar a pendência do acompanhamento no app independentemente do aviso.
- **FR-028**: Persistir definições, versões, opções, registros e respostas no armazenamento local com migração
  preservadora; sem dados fictícios em produção.
- **FR-029**: Verificar com datas/relógio controlados, migração após a cadeia completa, armazenamento, UI e aviso,
  relatando cobertura não executada.

### Key Entities

- **Tracker**: acompanhamento de um hábito; modo de frequência, data de criação, versão atual, estado ativo/
  removido, horário opcional de lembrete.
- **Tracker version**: estrutura imutável do formulário em um momento: perguntas ordenadas.
- **Question**: tipo, título, obrigatória, principal (escala), faixa e descrições dos extremos ou resposta única/
  múltipla, opções ordenadas; identidade estável entre versões.
- **Option**: rótulo e descrição curta; identidade estável entre versões; ausente nas versões em que foi removida.
- **Entry**: registro com data de referência, momento de preenchimento, versão respondida e respostas.
- **Answer**: valor de escala, opções escolhidas ou texto para uma pergunta da versão do registro.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um formulário com os quatro tipos de pergunta pode ser criado e preenchido em menos de 3 minutos.
- **SC-002**: Em 100% dos casos, preencher a mesma data duas vezes no modo um por dia resulta em um único registro.
- **SC-003**: 100% dos registros antigos mostram exatamente as perguntas, opções e rótulos de sua versão após
  qualquer sequência de edições do formulário.
- **SC-004**: Qualquer ação de acompanhamento deixa saldo, XP, metas, sequências, revisão e registros do hábito
  100% inalterados.
- **SC-005**: Após 3 dias desligado com lembrete diário, no máximo 1 aviso de acompanhamento é exibido.
- **SC-006**: Migração preserva 100% de tarefas, hábitos, razão financeira, recompensas, lembretes e cronômetros.
- **SC-007**: Dias sem registro aparecem como "Sem dados" em 100% das visões de calendário e análise.
- **SC-008**: Toda tarefa visível na busca por data anterior aparece na visão diária da mesma data.

## Assumptions

- Estado verificado em 2026-11-07: features 001–008 implementadas; Room schema 6; aba Calendário mapeada para
  `TaskPage.CALENDAR` com campo `yyyy-MM-dd`; detalhe do hábito com `HabitCalendar` mensal; lembretes em
  `core/reminders` com `ReminderKind { TASK, HABIT }` e título com nome da origem (o acompanhamento deve usar
  título neutro).
- Suposições mínimas de integração (revisáveis pelo dono): lembrete com um horário diário em datas em que o
  acompanhamento é preenchível; adiar reutiliza as opções existentes; no modo vários por dia, um registro
  válido na data resolve o aviso daquele dia; pendência não acumula avisos passados (regras de recuperação 007);
  exclusão individual com confirmação; ciclo de vida igual ao do hábito; o calendário mostra registros de
  hábito por data real, sem novas regras de agendamento; faixa alterada é exibida sem reescala.
- Opções e perguntas não vêm pré-preenchidas; não há sugestões de exemplo nesta entrega.
- Verificação anterior cobre só emulador API 36; API 24, dispositivo físico e TalkBack por áudio seguem não
  validados e serão relatados separadamente.
