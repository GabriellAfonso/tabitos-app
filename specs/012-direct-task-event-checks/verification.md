# Verificação — tarefas com marcação direta

## Alteração
- Checkbox envia SetCompleted com estado desejado e data de hoje. Nenhum diálogo de
  detalhes, checklist pendente ou efeito financeiro é aberto pelo checkbox.
- Escrita e contabilidade continuam na mesma transação. Estado desejado torna retry
  idempotente; snapshot confirmado é publicado antes de liberar o próximo toque.
- Concluídas ao fim da tela, após hábitos, inicialmente expandida e recolhível.
  Cartões feitos têm título riscado e cores discretas; checkbox marcado permite reabrir.
- Listas respeitam a lista atual e ordem manual. Hoje mostra conclusões com data atual;
  Revisão inclui as conclusões com data atual de tarefas vencidas. Filtro Hábitos oculta tarefas.
- Não realizadas permanece separada de Concluídas. Detalhes/notificações/Não será feita
  mantêm seus fluxos de confirmação. Desfazer recorrência preserva ações posteriores.
- Conforme esclarecimento do usuário, maus hábitos mantêm Registrar com confirmação.
  Não foi adicionado desfazer no cartão nem alterada a contagem de ocorrências.
- Sem migração, reset de dados, dependências, commits ou mudança de versão.

## Verificações
- 118 testes JVM aprovados: alternância rápida sem modal, falha/retry, datas/listas da
  seção Concluídas e confirmação/cancelamento do registro de mau hábito.
- Build e lint pelo Gradle Wrapper; integração Room e Compose no emulador API36.
  Build debug e APK de testes aprovados; lint com zero erros e 24 avisos já presentes.
- Integração inclui idempotência de conclusão/reabertura, ganho/estorno real e rollback
  ao tentar desfazer recorrência com ações independentes posteriores.
- Teste da interface usa Room e ViewModel reais: checkbox → Concluídas após hábitos →
  desmarcar → pendentes, com semântica marcado/desmarcado e carteira retornando ao valor anterior.
- Regressões dos detalhes/checklist, filtro de hábitos e navegação das configurações.
  Primeira execução Android: 4 testes aprovados (DirectTaskCheckTest, TasksContentTest,
  SettingsNavigationTest), 70,616 s.
  DirectTaskCheckboxTest aprovado em execução final (7,152 s): Concluídas abaixo dos hábitos,
  sem modais, desmarcação devolvendo a tarefa às pendentes e saldo/XP estornados.
  Total desta alteração: 118 testes JVM e 5 testes Android aprovados.
- `git diff --check`: sem erros de whitespace.

## Limites
- API24, TalkBack por áudio, R8/release e fontes ampliadas não revalidados nesta mudança.
- O celular API33 bloqueia toques remotos/APK separado de testes; integração executada
  no emulador. Instalação usa `adb install -r`, preservando os dados atuais.
  Instalação e abertura da MainActivity confirmadas no Xiaomi 2201117TG, API33.
