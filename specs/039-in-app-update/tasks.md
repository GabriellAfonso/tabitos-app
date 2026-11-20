---
description: "Tasks — 039 Aviso de atualização do app"
---

# Tasks: Aviso de atualização do app

**Input**: specs/039-in-app-update/ (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Tests**: Princípio V. Testes JVM com fake da fonte para as transições, a checagem única, recusa, falha, download
pendente e o bloqueio por cronômetro. Não há persistência afetada. O fluxo real é validado pela trilha de teste
interno da Play e fica registrado como não executado.

## Phase 1: Setup

- [X] T001 Adicionar a versão `playAppUpdate = "2.1.0"` e a biblioteca `play-app-update-ktx`
  (`com.google.android.play:app-update-ktx`) em `gradle/libs.versions.toml`.
- [X] T002 Adicionar `implementation(libs.play.app.update.ktx)` em `app/build.gradle.kts`.

## Phase 2: Foundational

- [X] T003 [P] Criar `StoreUpdate`, `AppUpdatePhase`, `AppUpdateEvent` e o objeto puro `AppUpdateRules.next(phase, event)`
  seguindo a tabela de data-model.md, em
  `app/src/main/java/com/tabitos/app/core/update/domain/AppUpdateModels.kt`.
- [X] T004 [P] Criar a interface `AppUpdateSource` conforme contracts/app-update-source.md, em
  `app/src/main/java/com/tabitos/app/core/update/data/AppUpdateSource.kt`.
- [X] T005 Criar o `PlayAppUpdateSource`, um `AppUpdateManager` envolvido por `requestAppUpdateInfo`, um `callbackFlow`
  do `InstallStateUpdatedListener`, `startUpdateFlowForResult` com `AppUpdateOptions(FLEXIBLE)` e
  `completeUpdate`, sem propagar exceções da loja. Arquivo:
  `app/src/main/java/com/tabitos/app/core/update/platform/PlayAppUpdateSource.kt`.
- [X] T006 Criar o `AppUpdateController` `@Singleton` com `phase: StateFlow`, `run()` (checagem única por processo
  seguida do acompanhamento de `installStates`), `launchPrompt`, `promptResult`, `dismiss` e `install`, em
  `app/src/main/java/com/tabitos/app/core/update/data/AppUpdateController.kt`.
- [X] T007 Criar o módulo Hilt que fornece o `AppUpdateManager` via `AppUpdateManagerFactory.create(@ApplicationContext)`
  e liga `PlayAppUpdateSource` a `AppUpdateSource`, em
  `app/src/main/java/com/tabitos/app/core/update/data/AppUpdateModule.kt`.
- [X] T008 Testar as transições e o controlador com uma fonte falsa: checagem única, `Available`→prompt,
  recusa→`DISMISSED` absorvente, `Downloaded` pendente→`READY`, falha→`IDLE`, exceção na checagem→nada. Arquivo:
  `app/src/test/java/com/tabitos/app/core/update/AppUpdateControllerTest.kt`.

## Phase 3: US1 — Saber que há versão nova (P1)

Teste independente: com uma fonte `Available`, o pedido é lançado uma vez, só depois de o Hoje ser mostrado. Recusar
não faz nada.

- [X] T009 [US1] Criar o `AppUpdateViewModel`. Ele espera `StartupSequence.awaitTodayShown()`, roda o controlador e
  expõe `StateFlow<AppUpdateUiState>` com `prompt`, além de `launchPrompt(launcher)` e `promptResult(accepted)`. Arquivo:
  `app/src/main/java/com/tabitos/app/features/update/AppUpdateViewModel.kt`.
- [X] T010 [US1] Criar o `AppUpdatePromptEffect`, que registra `rememberLauncherForActivityResult(StartIntentSenderForResult)`
  e, com `prompt` verdadeiro, chama `launchPrompt`. O resultado `RESULT_OK` vira `promptResult(true)`. Arquivo:
  `app/src/main/java/com/tabitos/app/features/update/AppUpdateStrip.kt`.

## Phase 4: US2 — Instalar com um toque (P1)

Teste independente: com a fase `READY`, a faixa mostra "Reiniciar" e "Agora não". "Agora não" esconde a faixa até o
fim do processo.

- [X] T011 [P] [US2] Criar a função pura `updateNotice(phase, timerRunning): UpdateNotice?` e o enum `UpdateNotice`
  (READY, WAITING_TIMER), em `app/src/main/java/com/tabitos/app/features/update/AppUpdatePresentation.kt`.
- [X] T012 [US2] Adicionar `notice` ao `AppUpdateUiState`, junto com as ações `install()` e `dismiss()`, em
  `app/src/main/java/com/tabitos/app/features/update/AppUpdateViewModel.kt`.
- [X] T013 [US2] Criar a faixa `AppUpdateStrip`. É um `Snackbar` sem tempo limite, com `surfaceContainerHighest` e os
  botões Reiniciar/Agora não. Inclui Previews claro/escuro com dados explícitos. Arquivo:
  `app/src/main/java/com/tabitos/app/features/update/AppUpdateStrip.kt`.
- [X] T014 [US2] Adicionar as strings `update_ready`, `update_restart`, `update_not_now` e `update_waiting_timer` em
  `app/src/main/res/values/strings.xml`.
- [X] T015 [US2] Conectar ao app. Em `TabitosApp.kt`, criar o `AppUpdateViewModel` com `viewModel()`, coletar o estado
  com `collectAsStateWithLifecycle`, chamar o efeito do prompt e pôr a faixa no slot `snackbar` do shell, numa `Column`
  com a `RescheduledSnackbar`. Arquivo: `app/src/main/java/com/tabitos/app/TabitosApp.kt`.

## Phase 5: US3 — Não perder o cronômetro (P2)

Teste independente: com a fase `READY` e um cronômetro contando, aparece a variante sem "Reiniciar". Quando o
cronômetro para, "Reiniciar" volta.

- [X] T016 [US3] Combinar a fase com `TimerRepository.observe().map { it.running != null }` no ViewModel. Se o repositório
  falhar, tratar como parado, igual ao `RewardsViewModel`: sem cronômetros legíveis, não há contagem a proteger. Arquivo:
  `app/src/main/java/com/tabitos/app/features/update/AppUpdateViewModel.kt`.
- [X] T017 [US3] Mostrar na faixa a variante WAITING_TIMER, com o texto de espera e só "Agora não". O `install()` do
  ViewModel não age com cronômetro contando. Arquivo: `app/src/main/java/com/tabitos/app/features/update/AppUpdateStrip.kt`.
- [X] T018 [P] [US3] Testar `updateNotice`: READY/WAITING_TIMER/null para cada fase, e a troca ao parar o cronômetro.
  Arquivo: `app/src/test/java/com/tabitos/app/features/update/AppUpdatePresentationTest.kt`.

## Phase 6: Polish

- [X] T019 Rodar `.\gradlew.bat :app:testDebugUnitTest --tests "com.tabitos.app.core.update.*" --tests "com.tabitos.app.features.update.*"`
  e corrigir as falhas.
- [X] T020 Registrar no `AGENTS.md` (lista de specs) e em `specs/039-in-app-update/verification.md` o que foi
  verificado. Registrar como não executados o fluxo fim a fim pela Play, build debug, lint e instrumentados.

## Dependencies

- O Setup (T001–T002) vem antes do Foundational (T003–T008).
- O Foundational vem antes de US1, US2 e US3.
- US1 (T009–T010) vem antes de US2, porque T012 estende o ViewModel de T009.
- US2 (T011–T015) vem antes de US3, porque T016–T017 estendem o ViewModel e a faixa.
- O Polish vem por último.

## Parallel

- T003 e T004 podem rodar juntas. T011 e T018 também.
- O restante toca arquivos compartilhados, como o ViewModel e `AppUpdateStrip.kt`, e deve ser sequencial.

## Implementation Strategy

O MVP é US1 + US2 (pedido e faixa). Logo depois vem US3 (proteção do cronômetro), que é curta e fecha o risco de
pausar um cronômetro. Tudo é entregue numa única rodada.
