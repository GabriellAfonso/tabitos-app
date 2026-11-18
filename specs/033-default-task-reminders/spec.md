# Lembretes padrão para tarefas novas

Feature 033 · 2026-11-18 · Especificada para planejamento · Branch de trabalho `master`.

## User Scenarios & Testing

### US1 · Receber um aviso ao criar uma tarefa com horário (P1)

Ao criar “Consulta” para amanhã às 15h, quero receber um aviso sem precisar abrir o sininho.

- Com as preferências iniciais, criar a tarefa agenda um único aviso para amanhã às 14h30.
- Criar uma tarefa só com data ou sem prazo deixa a tarefa sem aviso automático.
- Atualizar o app, abrir uma tarefa antiga, remarcar seu prazo ou gerar a próxima ocorrência de uma série
  antiga preserva os avisos escolhidos para ela. Nenhuma dessas ações conta como criar uma tarefa.

### US2 · Escolher os avisos que costumo usar (P2)

Em Configurações, quero escolher os lembretes uma vez para as tarefas que criar a partir dali.

- Com 1 dia e 30 minutos de antecedência selecionados, criar uma tarefa com horário e prazo a três dias
  agenda dois avisos. Uma tarefa criada anteriormente mantém os avisos que tinha.
- Escolher 9h para tarefas só com data e criar uma para amanhã agenda um aviso amanhã às 9h.
- Desligar todos os padrões e criar uma tarefa deixa o sininho disponível para configurar avisos à mão.

### US3 · Fazer uma exceção pelo sininho (P2)

Nos detalhes de uma tarefa, quero ver seus avisos, ajustar um horário, acrescentar um aviso ou silenciá-la.

- Trocar o aviso de 30 por 10 minutos antes afeta somente a tarefa escolhida, no escopo selecionado.
- Remover todos os avisos e reabrir ou remarcar a tarefa mantém a escolha de silêncio.
- Criar outra tarefa com horário continua aplicando o padrão das Configurações.

### Edge Cases

- Se o instante de um aviso já passou na criação, pular esse aviso e manter os demais que ainda são futuros.
- Remarcar uma tarefa com aviso relativo recalcula o aviso pelo novo prazo. Concluir a tarefa antes do aviso
  impede sua entrega. Os avisos seguem as regras locais de fuso, reinício e entrega da feature 007.
- Uma nova série recorrente recebe o padrão da criação para suas ocorrências. O sininho mantém os escopos
  existentes de alteração de uma ocorrência ou desta e futuras.

## Requirements

### Escopo e restrições do Ethos

O escopo é a criação de tarefas e a configuração dos seus avisos locais. Hábitos, acompanhamentos e resumo
diário seguem suas próprias preferências. A entrega depende das permissões de notificação existentes.
Os dados pessoais e os lembretes existentes devem ser preservados na atualização.

### Functional Requirements

- **FR-001** — A configuração inicial habilita apenas 30 minutos de antecedência para tarefas com data e hora.
- **FR-002** — Configurações permite escolher uma ou mais antecedências, inclusive 1 dia e 30 minutos,
  desativá-las e, separadamente, escolher ou desativar um horário local para tarefas só com data.
- **FR-003** — Aplicar a preferência vigente ao criar uma tarefa ou série conforme o prazo informado.
  Nenhuma preferência cria um prazo. Mudanças posteriores na preferência valem só para novas criações.
- **FR-004** — O sininho mostra os avisos da tarefa e permite adicionar, editar e remover cada um, inclusive
  o último. As escolhas persistem ao reabrir o app e prevalecem sobre o padrão usado na criação.
- **FR-005** — Respeitar as regras de série, remarcação, conclusão, fuso e recuperação dos lembretes
  existentes. Cada aviso futuro deve aparecer uma única vez na tarefa, inclusive após reinício.
- **FR-006** — As escolhas funcionam offline. Em falha de salvamento, informar o erro e permitir nova tentativa.
  Em caso de bloqueio de notificações, indicar o impedimento pelo fluxo existente.
- **FR-007** — Em português e com fonte ampliada ou leitor de tela, deve ser possível identificar o tipo de
  tarefa, quais avisos estão ligados e como alterar ou desativar cada um.

### Key Entities

- **Lembretes padrão de tarefas** — Antecedências para tarefas com data e hora e horário opcional para
  tarefas só com data. São preferências para a próxima criação.
- **Avisos da tarefa** — Lembretes que pertencem à tarefa ou série e que a pessoa pode ajustar nos detalhes.

## Success Criteria

- **SC-001** — Criar uma tarefa para amanhã às 15h com os padrões iniciais produz um único aviso agendado
  para 14h30, com zero ações no sininho.
- **SC-002** — Uma verificação com tarefas antigas com e sem lembretes e uma série antiga encontra zero
  avisos acrescentados pela atualização, pela troca do padrão ou pela geração da próxima ocorrência.
- **SC-003** — Após escolher dois avisos futuros, uma nova tarefa elegível mostra ambos. Silenciar essa
  tarefa e reiniciar o app mantém zero avisos nela e os dois na próxima tarefa elegível.
- **SC-004** — Para uma tarefa de amanhã só com data, a configuração inicial produz zero avisos. Com 9h
  selecionado antes da criação, aparece um aviso para amanhã às 9h.

## Assumptions

“Só as novas” se refere à criação da tarefa ou série. Uma tarefa criada sem prazo pode receber lembretes
manualmente depois. As preferências não são reaplicadas quando a pessoa edita o prazo.

Regras relacionadas em [Lembretes e notificações](../007-reminders-notifications/spec.md) e
[Remarcar tarefa](../032-reschedule-task/spec.md).
