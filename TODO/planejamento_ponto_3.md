# Ponto 3 — listas e tarefas

Registro consolidado de design. Prompt salvo em `TODO/specify_listas_tarefas_app.md`.
Este registro e o prompt não autorizam implementação nem executam Spec Kit.

## Contexto

- Ler `TODO/planejamento_ponto_2.md` para decisões de prazos, recorrências, revisão,
  dificuldade/importância e limites entre entregas.
- Interface aprovada: modelo B em `prototypes/hoje/index.html`.
- Prompt da estrutura visual: `TODO/specify_interface_navegacao_app.md`.

## Decisões aprovadas nesta conversa

- Título obrigatório e descrição opcional. Permitir mover tarefas entre listas ou para a
  Caixa de entrada sem alterar seu histórico.
- Nas listas de tarefas, mostrar pendentes primeiro e concluídas em uma seção recolhível.
  Permitir reordenar manualmente as tarefas pendentes.
- Em Hoje, tarefas com prazo semanal e sem dia definido aparecem abaixo das tarefas do dia
  em Nesta semana · sem dia definido, visíveis nos filtros Tudo/Tarefas. Podem ser concluídas
  em qualquer dia até o prazo, sem exigir agendamento em uma data específica.

- Horário é opcional e organiza a agenda. Passar do horário não marca descumprimento nem gera
  revisão imediata: a tarefa de dia específico entra em revisão quando esse dia terminar.
  Lembretes poderão avisar no horário, em entrega própria, sem tratar o aviso como falha.

- Listas têm nome, cor e ícone, com opções de renomear e reordenar.
- Tarefas podem ficar na Caixa de entrada sem pertencer a uma lista personalizada, até serem
  organizadas. Não obrigar a criação/escolha de uma lista para cadastrar uma tarefa.
- Novas tarefas começam com dificuldade Média e importância Média. Selecionar dificuldade
  Difícil sugere importância Alta, mas não sobrescreve uma importância já escolhida manualmente.
- Permitir Desfazer conclusão: reabrir a tarefa e reverter exatamente as moedas e o XP daquela
  conclusão. É correção de registro, não penalidade. Concluir novamente não pode gerar recompensa
  duplicada. Integração financeira pertence ao specify de gamificação; regra já aceita para o fluxo.
- Permitir desfazer Não será feita por engano: reabrir a ocorrência e devolver exatamente as
  moedas efetivamente descontadas naquele registro. Se o piso de −300 limitou o desconto,
  devolver apenas o desconto efetivo, não a penalidade nominal. Não duplicar estornos.
- Permitir informar conclusão retroativa de uma tarefa que já existia na data informada,
  diferenciando quando ela foi feita de quando o registro foi lançado. Não aceitar data de
  conclusão anterior à criação da tarefa. No modo X dias após concluir, usar a data real
  de conclusão informada para calcular a próxima ocorrência, não a data de lançamento do registro.
- Tarefas sem data ficam na Caixa de entrada ou lista escolhida, sem aparecer automaticamente
  em Hoje e sem gerar atraso ou penalidade por prazo. Definir um prazo faz a tarefa entrar na
  visão correspondente. Isso não altera as regras de tarefas com prazo semanal sem dia fixo.
- Em recorrências por calendário fixo, cada ocorrência vencida fica separada em Revisar pendências,
  inclusive quando a pessoa passa dias sem abrir o app. Não juntar nem descartar ocorrências e
  não aplicar descontos automáticos. Exemplo: diária de segunda a quarta gera três revisões.
- No modo X dias após concluir, manter uma única ocorrência pendente: não gerar a seguinte
  enquanto a atual não for concluída ou marcada Não será feita. Aplicar então as regras de
  cálculo da próxima data já registradas no planejamento do ponto 2.
- Recorrências têm data inicial e três opções de término: Nunca (até interromper), Em uma data
  ou Após X ocorrências. As três opções entram na primeira versão de tarefas.
- Após X ocorrências conta as ocorrências previstas, incluindo concluídas e marcadas Não será
  feita. Descumprimentos não prolongam a série para compensar falhas.
- Em uma data usa término inclusivo: uma ocorrência prevista na data final ainda acontece.
  Não gerar ocorrências posteriores ao término, mas preservar pendências anteriores para revisão.

- Incluir subtarefas/checklist já na primeira versão de listas e tarefas.
- Checklist simples dentro da tarefa: cada item pode ser marcado, mas não tem prazo,
  dificuldade, importância ou recompensa próprios. Moedas e XP ficam na tarefa principal,
  evitando recompensa dupla. Não implementar subtarefas completas nesta versão.
- Marcar todos os itens do checklist não conclui automaticamente a tarefa principal.
  A conclusão exige uma ação explícita separada do usuário.
- Se o usuário tentar concluir a tarefa principal com itens pendentes, mostrar a quantidade
  restante e pedir confirmação (ex.: Restam 2 itens. Concluir mesmo assim?). Permitir confirmar
  ou voltar ao checklist; não bloquear definitivamente a conclusão.
- Em tarefas recorrentes, cada nova ocorrência mantém os itens do checklist, mas começa com
  todos desmarcados. Marcações de uma ocorrência não concluem itens de outras ocorrências.
- Editar ou excluir uma tarefa recorrente oferece dois escopos: Só esta ocorrência ou
  Esta e as próximas. Preservar o histórico anterior; não modificar ocorrências passadas
  automaticamente ao ajustar a rotina futura.
- Excluir uma lista exclui também suas tarefas e hábitos; não mover os itens automaticamente
  para Sem lista. O usuário escolheu Excluir tudo, sem penalidade pela exclusão.
- Antes de excluir, pedir confirmação explícita de que a lista e todas as suas tarefas e hábitos
  serão excluídos. Preservar histórico de moedas e XP já registrados; excluir a lista não altera
  saldo, XP ou níveis nem estorna ganhos/penalidades anteriores.

## Campos consolidados

- Título obrigatório, descrição opcional, lista, prazo (sem data/dia/semana), horário opcional,
  repetição, dificuldade e importância.
- Os campos foram confirmados ao longo das decisões registradas acima; checklist também incluído.

## Questões abertas

- O usuário quer lembretes/notificações e tarefas com acompanhamento de tempo, incluindo metas
  semanais e recompensas temporais. Registrar como entregas próprias, sem implementar por
  consequência neste ponto. Detalhes em `TODO/planejamento_lembretes_tempo.md`.

- Detalhes de consistência para plano/clarify: fuso e mudanças de relógio, estornos com ocorrências
  seguintes já resolvidas, contagem de exceções excluídas e representação de revisão no reagendamento.
- Moedas/XP e seus estornos são regras de integração futura, sem implementar motor financeiro aqui.
