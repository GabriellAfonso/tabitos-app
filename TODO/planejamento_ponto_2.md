# Ponto 2 — registro da conversa, ainda não é prompt de specify

## Decisões e intenções expressas pelo usuário

- Hoje reúne tarefas e hábitos.
- Haverá hábitos negativos, com registro quando houver recaída/ocorrência.
- Tarefas devem permitir repetição diária, em dias úteis, em um dia semanal escolhido,
  mensal e a cada X dias; o objetivo é flexibilidade de configuração.
- O usuário quer avaliar a interface no navegador em mockups com proporção de celular.
- O usuário quer um menu para criar listas e organizar tarefas/hábitos/projetos, como Rotina,
  Trabalho ou outras listas personalizadas. Cada lista terá abas Tarefas e Hábitos.
- Escolha visual aprovada: B / Um passo de cada vez, lista compacta e filtros em Hoje.
- O usuário quer itens com prazo semanal sem dia fixo e acompanhamento de frequência/limite
  semanal. Diferenciar metas mínimas, máximas e exatas ainda requer definição.
- Direção de gamificação proposta pelo usuário: ganhar moedas ao concluir tarefas/hábitos,
  com dificuldade influenciando prêmio ou punição, e cadastrar recompensas com custo em moedas
  (ex.: jogar meia hora ou lanchar). Não tratar como moedas reais ou compra dentro do app.
- Atualização posterior: o usuário aprovou XP e níveis; rejeitou energia nesta etapa.
- XP é ganho ao cumprir tarefas e metas. Falhas não retiram XP nem níveis; consequências
  negativas ficam nas moedas. Resgatar recompensas não deve ser tratado como falha.
- Descumprimentos descontam moedas. O saldo pode ficar negativo até um piso, para que uma ausência
  prolongada não gere dívida impossível de recuperar em poucos dias; piso definido abaixo.
- Piso definido pelo usuário: −300 moedas. Descontos não podem reduzir o saldo abaixo desse valor.
  A possibilidade de editar o piso nas configurações ainda não foi aprovada.
- Resgatar recompensa exige saldo suficiente para pagar seu custo integral; resgates não podem
  gerar dívida. Saldo negativo só pode resultar de penalidades, respeitando o piso de −300.
- Dificuldade e importância serão campos separados para balancear recompensas e penalidades.
- Ao selecionar Difícil, o padrão deve ir para Importante; o usuário pode alterar a importância.
  A edição manual tem prioridade sobre sugestões posteriores.
- Ganhos e penalidades devem ficar claros; o usuário quer poder consultar a tabela ou ter uma
  apresentação equivalente explícita. A apresentação aprovada está descrita abaixo.
- Apresentação aprovada: mostrar ganhos em moedas/XP e penalidade em moedas na criação/edição,
  com acesso à tabela explicativa também disponível nas configurações. Valores ainda não definidos.
- Preservar importância editada manualmente ao mudar dificuldade; a sugestão automática de
  Importante ao selecionar Difícil não deve sobrescrever essa escolha manual.
- Níveis aprovados: dificuldade Fácil/Média/Difícil e importância Baixa/Média/Alta.
  O padrão sugerido ao selecionar Difícil é importância Alta, respeitando edição manual prévia.
- Direção de balanceamento aprovada: dificuldade aumenta o prêmio pelo esforço; importância
  aumenta a consequência de descumprir. A fórmula e os valores ainda não foram definidos.
- O usuário não quer penalidade automática apenas porque o prazo passou: pode ter feito a tarefa
  e esquecido de registrar. Tarefas precisam permitir concluir depois ou marcar Não será feita.
- Hábitos negativos: descontar moedas por ocorrência confirmada, permitindo múltiplos registros
  no mesmo dia, respeitando o piso global do saldo. Não limitar a uma penalidade diária.
  A interface deve mostrar o desconto antes da confirmação; reversão de registros ainda precisa
  de definição completa quanto a histórico e saldo.
- Hábitos com limite máximo por período: ocorrências dentro do limite não geram penalidade;
  cada ocorrência excedente gera um desconto individual, respeitando o piso global.
  Exemplo aprovado: máximo 1 delivery por semana → primeiro sem penalidade, segundo e seguintes
  com penalidade por ocorrência. Não confundir limite máximo com proibição total.
- Não será feita confirma descumprimento e aplica a penalidade prevista.
- Em tarefas recorrentes, Não será feita encerra e penaliza somente a ocorrência escolhida;
  a série e as próximas ocorrências continuam normalmente.
- Reagendar uma tarefa vencida exige revisar o descumprimento do prazo original antes de mudar
  a data. A passagem do tempo, por si só, continua sem autorizar desconto automático.
- Semana vai de segunda a domingo. Tarefas podem ter prazo Nesta semana (até domingo) ou
  Nesta semana — dias úteis (até sexta). Dias úteis significam segunda a sexta, sem feriados.
- Essas tarefas ficam sem dia definido durante o período; depois do prazo entram em revisão,
  sem desconto automático. A semana completa entra em revisão na segunda seguinte; o prazo
  de dias úteis entra em revisão após sexta-feira.
- Repetição mensal: se o dia escolhido não existir no mês, usar o último dia daquele mês.
  Preservar o dia originalmente escolhido como referência para os meses seguintes
  (ex.: dia 31 → 28/29 de fevereiro → 31 de março).
- Repetição A cada X dias terá dois modos: calendário fixo, independente da conclusão,
  ou intervalo após a conclusão. No segundo modo, a próxima data é calculada a partir
  da data de conclusão informada, inclusive em registro retroativo.
- No modo X dias após concluir, marcar Não será feita mantém a série ativa e agenda a próxima
  para X dias após a data prevista da ocorrência encerrada, não após o dia em que ela foi revisada.
- Se a tarefa deixou de fazer sentido, a pessoa a exclui, sem precisar marcar Não será feita
  e sem penalidade por essa exclusão. Não criar uma ação separada Cancelar sem penalidade.
- Atualização: aprovada uma aba única Revisar pendências, reunindo tarefas e hábitos, com filtros
  Tudo/Tarefas/Hábitos. Permitir corrigir registros esquecidos e confirmar descumprimentos;
  penalidades por prazo/meta não cumpridos dependem de confirmação, não só da passagem do tempo.
- Posição aprovada: Revisar pendências no menu lateral, com contador; Hoje mostra um aviso discreto
  com a quantidade pendente e acesso à revisão. Não adicionar outro item à barra inferior.
- A lógica discutida é premiar maior esforço com mais moedas e cobrar mais tarefas fáceis não feitas;
  a influência da importância e os valores finais precisam de balanceamento. A tabela de exemplo
  do assistente (+5/+10/+20 e −10/−5/−3) NÃO foi aprovada como regra definitiva.
- Navegador preferido: Brave, perfil pessoal. Não assumir qual diretório é o perfil sem verificar.

## Limites

Esses requisitos orientam o design. Não significam implementar recorrência e hábitos no ponto 2.
Navegação, aparência e preferências pertencem à estrutura visual; mecanismos de tarefas e hábitos
terão especificações próprias. A divisão final ainda deve ser discutida.

## Protótipo exploratório

Rodada 02: modelo B com saldo/nível no topo de Hoje e acesso a Recompensas e Histórico de moedas
no menu lateral. O usuário aceitou experimentar essa posição e pediu para visualizá-la.
Três celulares mostram Hoje, Recompensas e Revisar pendências como estados independentes do mesmo
design. Listas incluem abas Tarefas/Hábitos. Resgates e revisões pedem confirmação; os saldos
mudam apenas na memória da página. Os valores +10 moedas/+15 XP/−5 moedas, preços das recompensas,
saldo inicial de 120 e nível 4 são exemplos visuais, não parâmetros aprovados de balanceamento.

`prototypes/hoje/index.html`: comparação da fundação atual com três opções de Hoje, dados fictícios,
temas, navegação e ações simuladas. Organização B aprovada; paleta e demais controles ainda propostos.
Não altera código Kotlin, não salva dados e não calcula recorrências.

## Estado consolidado e próximos specifies

O usuário aprovou a aparência da rodada 02 e pediu para salvar o prompt do ponto 2.
Prompt: `TODO/specify_interface_navegacao_app.md`. Gerar esse arquivo não executa Spec Kit.

Separação proposta para as entregas seguintes:
- Ponto 2: interface/navegação, temas persistidos e estados vazios honestos; sem motores funcionais.
- Ponto 3: listas/tarefas, Room, edição, conclusão, prazos e recorrências.
- Ponto 4: hábitos e metas.
- Ponto 5: moedas, XP, níveis e histórico contábil.
- Ponto 6: cadastro e resgate de recompensas.

## Questões ainda abertas

- Ponto 3: campos das listas/tarefas, organização, exclusão/edição de série versus ocorrência,
  efeitos do reagendamento e reversão de conclusão, datas/fuso e gerações atrasadas de recorrências.
- Hábitos: metas mínimas/exatas, registro retroativo e revisão dos períodos; histórico e correções.
- Gamificação: fórmulas de dificuldade/importância, XP por nível, parametrização, estornos e piso editável.
- Recompensas: cadastro/edição, cancelamento de resgate e eventuais limites.
- Calendário: comportamento funcional completo; projeto como entidade própria ainda não decidido.

Posições e estrutura visual da rodada 02 estão aprovadas. Números de exemplo não são balanceamento.