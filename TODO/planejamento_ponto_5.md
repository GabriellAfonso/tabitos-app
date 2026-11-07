# Ponto 5 — moedas, XP e níveis

Registro de design consolidado. Prompt salvo em `TODO/specify_moedas_xp_niveis_app.md`.
Salvar o prompt não executa Spec Kit nem autoriza implementação.

## Decisões anteriores

- Moedas são ganhas por tarefas/hábitos e gastas em recompensas pessoais, sem dinheiro real.
- Sem energia. XP e níveis representam progresso; falhas não retiram XP nem níveis.
- Penalidades afetam moedas, com piso de −300; não aplicar desconto só porque passou o prazo.
- Recompensas exigem saldo suficiente e não podem criar dívida.
- Correções revertem lançamentos correspondentes sem duplicação. Reversão de penalidade devolve
  o desconto efetivo, considerando o piso, não um valor nominal que não chegou a ser descontado.
- Dificuldade: Fácil/Média/Difícil. Importância: Baixa/Média/Alta; padrão Média/Média.
- Difícil sugere importância Alta, sem sobrescrever escolha manual.
- Exibir ganhos e penalidades nos formulários e permitir consultar a tabela nas configurações.
- Consultar planejamentos e specs de tarefas/hábitos para identidade, revisão e correções.

## Decisões aprovadas nesta rodada

- Separação direta: dificuldade determina moedas e XP ganhos ao cumprir; importância determina
  moedas perdidas ao confirmar descumprimento. Não combinar os dois campos em ambas as fórmulas.
- Assim, difícil/baixa importância pode render prêmio alto e penalidade baixa; fácil/alta
  importância pode render prêmio baixo e penalidade alta.
- Tabela inicial aprovada de ganhos por dificuldade: Fácil +5 moedas/+10 XP;
  Média +10 moedas/+20 XP; Difícil +20 moedas/+40 XP.
- Tabela inicial aprovada de penalidades por importância: Baixa −5 moedas;
  Média −10 moedas; Alta −20 moedas. Falhas não descontam XP.
- Hábitos de prática premiam cada dia válido registrado, em vez de esperar atingir a meta
  semanal/mensal para pagar tudo. A quantidade de registros no mesmo dia não pode multiplicar
  os dias válidos.
- Ao atingir a meta de prática, não aceitar novas práticas naquele período e mover o hábito
  para Metas cumpridas. Não premiar dias extras nem conceder bônus de fechamento.
  Permitir corrigir/desfazer registros; se a meta ficar incompleta, aceitar novas práticas
  novamente, sem duplicar recompensas. Evitar/limitar continuam aceitando ocorrências excedentes.
- Evitar/limitar: premiar uma única vez ao fechar o período, após confirmação de registros
  completos, se ficou dentro da meta. Se ultrapassou o limite, não há prêmio de fechamento;
  aplicar apenas as penalidades de cada ocorrência proibida/excedente, sem penalidade duplicada
  pelo fechamento. Ganho segue dificuldade; perda por ocorrência segue importância.
- Prática com meta incompleta confirmada: manter os ganhos dos dias cumpridos e aplicar uma
  única penalidade por período conforme a importância, não uma penalidade por dia faltante.
- Início: zero moedas, zero XP e nível 1. Decisão final de progressão: voltar à proposta linear,
  sem multiplicador. Do nível 1 ao 2 são 100 XP adicionais; cada etapa seguinte exige 50 XP
  adicionais a mais que a anterior. Custo do nível n para n+1 = 100 + 50 × (n−1).
  Exemplos: 100, 150, 200, 250; do 99 ao 100, 5.000 XP adicionais e 252.450 XP acumulados
  desde o nível 1. As propostas de multiplicador, arredondamento e duplicação foram descartadas.
  O exemplo do nível 100 não estabelece limite máximo de nível.
- Desfazer ganho já gasto pode gerar dívida, limitada a −300. Se o estorno ultrapassar o piso,
  parar em −300 sem dívida escondida. Remover o XP daquele ganho pode reduzir o nível por ser
  correção de registro, não punição por falha. Registrar valor efetivo e não duplicar estornos.
- Ganhos e penalidades podem ser editados nas configurações. Alterações valem para novas
  conclusões/ocorrências; registros anteriores e seus estornos mantêm os valores originais.
- Carteira começa em zero. Histórico mostra ganhos, penalidades, resgates e estornos com motivo,
  data, valor efetivo e saldo resultante. Não oferecer botão de criação manual de moedas.

## Questões abertas

- Valores de ganhos/penalidades e progressão aprovados acima substituem exemplos antigos;
  valores ilustrativos do protótipo não são regras. Ao consolidar o prompt, verificar consistência
  das correções, histórico e integrações sem criar novas funcionalidades não discutidas.
- Detalhes para clarify/plan: ativação diante de histórico anterior à carteira, preço de registro
  retroativo ao mudar a tabela, validação dos valores editáveis e cadeias de correção com estorno
  limitado pelo piso. Não assumir pagamento/cobrança retroativa de todo o histórico existente.
