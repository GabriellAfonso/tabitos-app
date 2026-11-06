# Ponto 4 — hábitos e metas

Conversa de design em andamento. Não é prompt de specify e não autoriza implementação.

## Contexto e decisões anteriores

- Ver `TODO/planejamento_ponto_2.md` para revisão, gamificação e limites por ocorrência.
- Hábitos aparecem em Hoje e na aba Hábitos de cada lista, respeitando os filtros aprovados.
- Tipos discutidos: praticar, evitar e limitar ocorrências. Penalidades e recompensas reais
  dependem do specify de gamificação; não importar valores ilustrativos do protótipo.
- Hábitos negativos permitem múltiplas ocorrências no mesmo dia, com penalidade por ocorrência.
- Em metas de limite máximo, registros dentro do limite não são penalizados; cada excedente é.
- Falhas por prazo/meta dependem de revisão; registros esquecidos podem ser corrigidos.
- Metas de duração e cronômetros ficam em entrega própria: `planejamento_lembretes_tempo.md`.

## Decisões aprovadas nesta rodada

- Hábitos podem existir sem lista personalizada, na Caixa de entrada. Não obrigar a criação
  ou escolha de lista para cadastrar um hábito.
- Campos aprovados: nome obrigatório, descrição opcional, cor, ícone, lista opcional,
  dificuldade, importância, tipo e meta/configuração correspondente.
- Dificuldade e importância começam em Média. Selecionar Difícil sugere importância Alta,
  preservando qualquer importância já ajustada manualmente, como nas tarefas.
- Hábitos de prática semanais/mensais sem dias fixos aparecem em Hoje enquanto a meta estiver
  incompleta, com progresso do período (ex.: 2 de 3 dias). Ao atingir a meta, ficam na seção
  recolhível Metas cumpridas até o próximo período. Respeitar os filtros Tudo/Hábitos de Hoje.
- Hábitos ativos de evitar/limitar permanecem visíveis em Hoje durante todo o período, mesmo
  após ultrapassar o limite, permitindo continuar registrando ocorrências. Não escondê-los
  automaticamente após falha nem transferi-los para Metas cumpridas por falta de registros.
- Excluir um hábito não apaga seu histórico nem registros passados. Retirar do acompanhamento
  ativo, preservando consulta histórica. A exclusão não aplica penalidade nem estorna ganhos,
  descontos ou XP já registrados. O usuário corrigiu explicitamente a proposta de apagar registros.
  Preservar essa regra também quando a exclusão decorrer de excluir a lista que contém o hábito;
  Excluir tudo refere-se aos itens ativos, não à eliminação dos históricos preservados.
- Histórico terá uma seção Hábitos excluídos para consultar esses hábitos e restaurá-los.
  Não apagar registros ao excluir.
- Ao restaurar, tratar o intervalo excluído como pausa: sem cobrança retroativa e preservando
  histórico anterior. Se retornar no meio da semana/mês, permitir ajustar a meta desse período,
  seguindo as regras de retomada. Restaurar não gera recompensas duplicadas.
- Permitir corrigir ou excluir registros de hábitos feitos por engano, recalculando o progresso
  da meta/período afetado. Quando a integração de moedas/XP existir, ajustar os lançamentos
  correspondentes sem duplicar ganhos, descontos ou estornos. Fórmulas e detalhes contábeis
  permanecem no specify de gamificação; a correção não autoriza um motor financeiro nesta entrega.
- Permitir registros retroativos (ex.: Fiz ontem ou outra data), sem aceitar data anterior à
  criação do hábito nem futura. Contabilizar na data/período real da prática ou ocorrência,
  diferenciando essa data do momento em que o registro foi inserido.
- Incluir Pausar hábito já na primeira versão. Preservar histórico; durante a pausa não cobrar
  metas nem gerar novas pendências relativas à pausa.
- Ao pausar ou retomar no meio de semana/mês, permitir ajustar a meta desse período parcialmente
  ativo, seguindo o modelo de ajuste inicial. Não alterar silenciosamente o objetivo. Pendências
  de períodos anteriores à pausa continuam disponíveis para revisão.
- Ao editar a meta, oferecer A partir do próximo período ou Já neste período. Na segunda opção,
  mostrar o efeito no progresso recalculado antes de confirmar. Preservar períodos anteriores;
  não reescrever retroativamente metas históricas por uma edição da configuração atual.
- Histórico inclui calendário de registros e progresso por período (ex.: 3 de 4 dias na semana).
- Incluir sequência de metas cumpridas, separada de XP/níveis: contar dias para hábitos diários,
  semanas para semanais e meses para mensais.
- Sequência: pausa ou dia não previsto não soma nem quebra; período aguardando revisão permanece
  pendente sem quebra automática; descumprimento confirmado quebra a sequência atual e preserva
  o recorde anterior; correção de registros recalcula a sequência. Não apresentar continuidade
  através de um período ainda pendente como se estivesse confirmada.
- Hábitos de limitar permitem escolher o período do limite: dia, semana ou mês.
  A contagem de limite é por ocorrência, conforme as regras já aprovadas; não por dias distintos.
- Ao criar um hábito de limite no meio do período, permitir ajustar o limite apenas para esse
  primeiro período incompleto; nos períodos seguintes usar o limite normal. Ex.: limite mensal
  normal de quatro ocorrências com um limite inicial escolhido separadamente. Não presumir
  proporcionalidade automática nem aplicar a validação por dias distintos a limites de ocorrências.
- Hábitos de prática oferecem dias definidos (ex.: segunda, quarta e sexta) ou quantidade livre
  no período (ex.: três vezes na semana, em quaisquer dias).
- Metas semanais de prática contam dias distintos. Ex.: três vezes por semana exige prática
  em três dias diferentes; vários registros no mesmo dia não avançam a meta por vários dias.
  Isso não muda a contagem por ocorrência dos hábitos negativos ou de limite máximo.
- Em hábitos com dias definidos, praticar em outro dia não substitui automaticamente um dia
  previsto não cumprido. Esse dia fica para revisão: corrigir um registro esquecido ou confirmar
  descumprimento. Não aplicar penalidade automática apenas pela ausência de registro.
- Hábitos de quantidade semanal livre usam segunda a domingo. Se a meta não for atingida,
  gerar uma única pendência de revisão da semana inteira com progresso (ex.: 2 de 3 dias cumpridos),
  não uma pendência por dia faltante. A confirmação pode corrigir registros ou reconhecer a falha.
- Incluir metas mensais de prática já na primeira versão, contando dias distintos no mês
  (ex.: praticar em dez dias diferentes). Além delas, incluir metas diárias/semanais e dias
  da semana definidos. Não confundir metas por dias com contagem de ocorrências negativas.
- Metas mensais usam o mês do calendário, do primeiro ao último dia. Se não forem atingidas,
  gerar uma única pendência de revisão do mês com progresso (ex.: 8 de 10 dias), seguindo
  a lógica semanal, sem penalidade automática pela passagem do tempo.
- Ao criar uma meta semanal/mensal no meio do período, permitir ajustar manualmente a meta
  do primeiro período incompleto, em vez de adiar toda cobrança para o próximo período.
  Ex.: criado dia 25, meta normal dez dias/mês, primeira meta escolhida de três dias até o fim
  do mês. Nos períodos completos seguintes passa a valer a meta normal. Não assumir fórmula
  proporcional automática; sugestão/validação da primeira meta ainda precisa ser definida.
- Avisar quando a primeira meta for impossível por exigir mais dias distintos do que restam
  no período (ex.: cinco dias exigidos, três disponíveis) e pedir ajuste. Não tratar esse aviso
  como autorização para alterar automaticamente a quantidade escolhida pelo usuário.

## Questões a discutir

- Períodos permitidos, metas por dia, registros retroativos e confirmação na revisão.
- Histórico, edição/pausa do hábito e escopo das alterações.
