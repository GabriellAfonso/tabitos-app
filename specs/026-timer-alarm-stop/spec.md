# Spec 026 — Parar o alarme do cronômetro

Estado: implementado (2026-11-17). Protótipo: `prototypes/alarme-cronometro/index.html`, opção A escolhida.

## Problema

Com "Alarme ao terminar o cronômetro" ligado (spec 016), o fim do tempo de uma tarefa ou hábito postava uma
notificação no canal de alarme sem nenhuma ação. O som tocava até acabar sozinho e não havia como pará-lo,
nem pela notificação nem dentro do app.

## Comportamento

- A notificação que toca o alarme mostra **Parar alarme** e **Abrir**.
  - A versão pública (tela bloqueada com conteúdo oculto) diz "Tempo do cronômetro concluído." e também tem
    **Parar alarme**.
  - Parar alarme remove a notificação, o que silencia o som.
  - Abrir (ação ou toque na notificação) também silencia e abre a folha da sessão, como antes.
  - Deslizar a notificação para fora também conta como parar.
- Com o Ethos aberto, aparece um pop-up "Tempo concluído" com o nome do item, "Marcado como feito." e o botão
  **Parar alarme**. Fechar o pop-up de qualquer forma (botão, voltar, tocar fora) para o alarme.
- Parar o alarme só cala o som: a conclusão automática, o "Concluída pelo cronômetro · Desfazer" e as regras de
  moedas/XP continuam iguais.
- Só o fim de meta de tarefa/hábito toca alarme; tempo comprado de recompensa continua com aviso silencioso.

## Decisões técnicas

- O som continua sendo o do canal `ethos_timer_alarms` (sem player próprio nem serviço novo); a duração do toque
  continua sendo a do som de alarme do sistema.
- O estado "alarme tocando" fica só em memória no `TimerCoordinator`. Se o processo morrer, o pop-up não volta,
  mas a notificação continua com Parar alarme.
- Sem mudança de schema, permissão ou dependência.

## Fora do escopo / pendências

- Tela de alarme cheia na tela bloqueada (opção B) e folha da sessão no lugar do pop-up (opção C).
- Repetir o som até parar ou limitar a duração do toque (ex.: parar sozinho após 1 minuto).
- Com o Ethos aberto, a notificação do sistema ainda aparece no topo junto do pop-up, porque é ela que toca o som.

## Verificação

- `assembleDebug`, `testDebugUnitTest` e `lintDebug` aprovados; sem novos avisos de lint.
- Teste instrumentado `TimerAlarmNotificationTest` escrito e compilado, não executado.
