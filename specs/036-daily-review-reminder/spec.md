# Lembrete de revisão

**Feature 036 · 19/11/2026**  
Origem: [decisão de interface](../../TODO/lembrete_de_revisao.md). Esta feature substitui o resumo diário da spec 007.

## User Scenarios & Testing

### US1 · Lembrar de registrar o dia (P1)

Quem tem algo para registrar em Hoje recebe uma pergunta no horário local escolhido. Com tudo marcado ou num dia vazio,
a passagem do horário é silenciosa. Criar ou desmarcar um item depois disso não dispara o aviso.

### US2 · Abrir Hoje ou deixar para depois (P1)

O aviso permanece no painel. Um toque abre Hoje e encerra o aviso. Uma dispensa permitida pelo Android também encerra
a entrega do dia. Se ficar no painel, o aviso expira na virada do dia local. O dia seguinte tem seu próprio horário.

### US3 · Entender a opção em Configurações (P2)

A opção facultativa se chama **Lembrete de revisão**, usa o ícone de checklist e exibe a descrição
“Avisa se faltar marcar algo hoje.” Quem já ativou o resumo conserva o horário configurado.

## Requirements

- FR-001. No horário de entrega verificar as tarefas abertas de Hoje e os bons hábitos disponíveis para registro
  nessa data sem marcação vigente. Tarefas apenas em Revisar, hábitos pausados, excluídos ou de evitar/limitar
  não geram o aviso.
- FR-002. Mostrar “Ficou algo sem marcar hoje?” e “Toque para abrir Hoje e marcar as tarefas e os hábitos que você fez.”
  O texto preserva a privacidade e funciona sem contagens, nomes ou conexão.
- FR-003. Entregar no máximo uma vez por data, respeitar toque, dispensa e meia-noite. Reinício, reabertura,
  mudança de fuso e alteração de itens não devem recuperar um aviso de horário já perdido.
- FR-004. Tocar no aviso abre Hoje inclusive com o app já aberto em outra página.
- FR-005. Em Configurações, permitir ler a descrição com fonte ampliada e leitor de tela, alterar o horário
  e desligar o aviso. Preservar a rotina e a preferência existentes.

## Success Criteria

- Uma entrega no dia elegível, zero entregas no dia vazio, no dia já marcado ou ao adicionar uma tarefa após pular.
- Um toque chega a Hoje. Uma notificação de ontem não ocupa o painel no próximo dia.
- Uma atualização mantém a escolha de cada pessoa, inclusive quando o lembrete estava desligado.

## Assumptions

A referência de elegibilidade é Hoje com o filtro Tudo. O Android pode atrasar alarmes aproximados e permite a
dispensa individual de notificações contínuas em versões recentes. A recuperação considera o horário e fuso locais.
