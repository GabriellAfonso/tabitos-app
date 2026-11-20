# Prazo "Até uma data"

**Feature Branch**: `035-deadline-until-date`

**Created**: 2026-11-19

**Status**: Draft

**Input**: User description: "Prazo tem até domingo e até sexta, mas não tem como escolher até um dia específico."
Decisões combinadas na conversa: o período começa hoje por padrão (início editável); horário no fim é opcional;
esse prazo não se repete; em Hoje aparece no mesmo grupo de "Até domingo"/"Até sexta".

## Contexto verificado

- O seletor Prazo do editor de tarefa (spec 013/029) oferece Sem data, Dia específico, Até domingo e Até sexta.
- Dia específico aparece em Hoje somente na data e pode ter horário. Até domingo/Até sexta aparecem em Hoje todos
  os dias da semana corrente até o fim do período, num grupo separado das tarefas do dia, sem horário; depois do
  fim vão para Revisar.
- Cada ocorrência de tarefa já guarda início e fim do período separadamente; o tipo de prazo é gravado pelo nome.
- Remarcar (specs 021/032) e lembretes padrão (spec 033) hoje só se aplicam a Dia específico.
- O usuário já usa o app com sua rotina real: tarefas existentes devem continuar idênticas.

## User Scenarios & Testing *(mandatory)*

### US1 · Criar uma tarefa com prazo até uma data (P1)

Quero criar "Entregar relatório" com prazo até 28 nov, para ela ficar visível em Hoje todo dia até lá.

**Why this priority**: É o pedido em si; sem isso a semana fixa é a única janela possível.

**Independent Test**: Criar a tarefa em 19 nov com fim 28 nov e conferir Hoje em 19, 25 e 28 nov e Revisar em 29 nov.

**Acceptance Scenarios**:

1. **Given** o editor de tarefa aberto em 19 nov, **When** escolho Prazo → "Até uma data" e o dia 28 nov no
   calendário, **Then** o chip de Prazo mostra "Até 28 nov" e o início fica em Hoje (19 nov) sem eu escolher.
2. **Given** a tarefa salva, **When** abro Hoje em qualquer dia de 19 a 28 nov, **Then** ela aparece no mesmo grupo
   das tarefas "Até domingo"/"Até sexta".
3. **Given** a tarefa aberta e não concluída, **When** chega 29 nov, **Then** ela sai de Hoje e aparece em Revisar
   como as demais tarefas vencidas.
4. **Given** a tarefa concluída em 25 nov, **When** abro Estatísticas, **Then** ela conta como concluída no prazo.

---

### US2 · Definir horário e início (P2)

Às vezes o prazo tem hora ("até 28 nov às 18h") ou só começa depois ("de 24 a 28 nov").

**Why this priority**: Refina US1; a maioria dos usos funciona com início hoje e sem horário.

**Independent Test**: Criar uma tarefa com início 24 nov e fim 28 nov 18:00 e conferir Hoje em 23 e 24 nov e o
resumo "Até 28 nov 18:00".

**Acceptance Scenarios**:

1. **Given** "Até uma data" escolhido, **When** escolho um horário, **Then** o resumo mostra "Até 28 nov 18:00";
   sem horário, o prazo vale até o fim do dia.
2. **Given** "Até uma data" escolhido, **When** troco o início para 24 nov, **Then** a tarefa não aparece em Hoje
   antes de 24 nov e aparece de 24 a 28 nov.
3. **Given** o seletor de fim, **When** abro o calendário, **Then** dias anteriores ao início não podem ser
   escolhidos; o início pode ser qualquer dia a partir de hoje e, se passar do fim, o fim acompanha o início
   (correção de 2026-11-19: limitar o início ao fim o prendia em hoje enquanto o fim ainda era o padrão).

---

### US3 · Funcionar com o resto do app (P3)

A tarefa com prazo até uma data deve se comportar de forma coerente no calendário, nos lembretes e em Remarcar.

**Independent Test**: Com uma tarefa de 19 a 28 nov 18:00, conferir o Calendário, o lembrete padrão e Remarcar.

**Acceptance Scenarios**:

1. **Given** a tarefa de 19 a 28 nov, **When** abro o Calendário em qualquer dia do período, **Then** ela aparece
   naquele dia, como as tarefas semanais.
2. **Given** lembretes padrão (spec 033) ligados, **When** crio a tarefa com horário no fim, **Then** os avisos são
   calculados a partir do fim (ex.: 30 min antes de 28 nov 18:00); sem horário, usa o horário padrão de tarefas só
   com data no dia final, se estiver ativo.
3. **Given** a tarefa aberta, **When** uso Remarcar nos detalhes, **Then** escolho uma nova data final (Hoje/Amanhã/
   calendário), o horário é preservado, o início não muda e, se a nova data final for anterior ao início, o
   período começa na nova data final; Desfazer volta ao período anterior.

### Edge Cases

- Fim igual ao início (ex.: hoje até hoje): permitido; a tarefa aparece só nesse dia, no grupo de período.
- Início no passado ao editar uma tarefa existente: mantido; só o padrão para tarefas novas é hoje.
- Tarefa recorrente: a opção "Até uma data" não aparece no seletor de Prazo; com "Até uma data" escolhido, o chip/
  linha Repetir fica indisponível e explica que esse prazo não se repete.
- Trocar de "Até uma data" para outro tipo: Dia específico usa a data final como dia e mantém o horário; Até
  domingo/sexta descartam início e horário; Sem data descarta tudo.
- Trocar de outro tipo para "Até uma data": a data já escolhida vira a data final (ou hoje, se não houver).
- Concluir depois do horário final mas no mesmo dia segue a mesma regra de "no prazo" de Dia específico com horário.
- Tarefa vencida editada: segue a confirmação de revisão existente quando o fim muda.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- Escopo: novo tipo de prazo para tarefas únicas, com início, data final e horário opcional; integração com Hoje,
  Revisar, Calendário, Estatísticas, lembretes padrão e Remarcar.
- Exclusões: repetição desse tipo; mudança em Até domingo/Até sexta/Dia específico; hábitos.
- Offline e local; dados gravados no banco do app com as demais tarefas. Tarefas existentes não mudam.
- Se a gravação exigir mudança de estrutura do banco, a migração deve preservar todas as tarefas, séries,
  históricos e lembretes; sem recriar dados.
- Acessibilidade: seletores de data/hora existentes (sem digitação), rótulos lidos pelo TalkBack ("Até 28 de
  novembro às 18h"), suporte a fonte grande.
- Permissões e dados pessoais: N/A (nenhuma permissão nova; nenhum conteúdo de tarefa em logs).
- Datas e horários no fuso local do aparelho, como as demais tarefas.

### Functional Requirements

- **FR-001**: O seletor de Prazo MUST oferecer "Até uma data" para tarefas não recorrentes, depois de Até sexta.
- **FR-002**: Ao escolher "Até uma data" em uma tarefa nova, o início MUST ser hoje por padrão e a data final MUST
  ser escolhida pelo calendário; o início MUST poder ser alterado.
- **FR-003**: O usuário MUST poder escolher um horário opcional para o fim; sem horário, o prazo vale até o fim do
  dia final.
- **FR-004**: O sistema MUST rejeitar data final anterior ao início (os seletores não oferecem essas datas).
- **FR-005**: Tarefas com "Até uma data" MUST NOT ser recorrentes: o tipo não aparece para tarefas recorrentes e a
  repetição não é oferecida enquanto ele estiver escolhido.
- **FR-006**: Hoje MUST mostrar a tarefa aberta em todos os dias do início ao fim, no mesmo grupo de Até domingo/
  Até sexta.
- **FR-007**: Depois da data final, a tarefa aberta MUST ir para Revisar, com as mesmas opções das demais vencidas.
- **FR-008**: O Calendário MUST mostrar a tarefa em cada dia do período, com os mesmos filtros existentes.
- **FR-009**: Estatísticas MUST considerar "no prazo" a conclusão até a data final, pela mesma regra de Dia
  específico (incluindo horário, quando houver).
- **FR-010**: Lembretes padrão (spec 033) MUST tratar a data final e o horário opcional como Dia específico trata
  sua data e horário.
- **FR-011**: Remarcar MUST estar disponível e mudar a data final preservando horário e o início escolhido; se a
  nova data final for anterior ao início, o período passa a começar na própria data final; desfazer restaura o
  período anterior.
- **FR-012**: Resumos do prazo (chip, linha, cartão, pop-up de detalhes) MUST seguir o formato de datas pt-BR do app
  ("Até 28 nov", "Até 28 nov 18:00", ano só se diferente); textos em `strings.xml`.
- **FR-013**: Tarefas existentes com Sem data, Dia específico, Até domingo e Até sexta MUST manter exatamente o
  comportamento e os dados atuais.

### Key Entities

- **Prazo da tarefa**: tipo (Sem data, Dia específico, Até domingo, Até sexta, Até uma data), início do período,
  data final e horário opcional. "Até uma data" é o único tipo em que o usuário escolhe início e fim livremente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Criar uma tarefa "até uma data" leva no máximo 3 toques além do título (Prazo → Até uma data → dia).
- **SC-002**: Em 100% dos dias entre início e fim a tarefa aberta aparece em Hoje; no dia seguinte ao fim aparece
  em Revisar.
- **SC-003**: Após a atualização, 100% das tarefas existentes mantêm tipo, datas, horário, lembretes e histórico.
- **SC-004**: Nenhum fluxo permite salvar uma tarefa recorrente com "Até uma data" ou com fim antes do início.

## Assumptions

- Remarcar muda o fim, não o período inteiro (FR-011): é a leitura mais próxima de "adiar o prazo"; a ser revisto
  na clarificação se o usuário preferir deslocar início e fim juntos.
- O nome da opção é "Até uma data"; depois de escolhida, o resumo mostra a data ("Até 28 nov").
- Lembretes configurados à mão (sininho) seguem relativos ao fim, como em Dia específico.
- O tipo é acrescentado ao fim da lista de tipos gravados, sem renomear os existentes.
