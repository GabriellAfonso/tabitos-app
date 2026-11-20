# Implementation Plan: Aviso de atualização do app

**Branch**: master (sem branch própria) | **Date**: 2026-11-20 | **Spec**: [spec.md](spec.md)

## Summary

Ao abrir o app, depois que o Hoje aparece, o app pergunta à Play Store se há uma versão nova. Se houver, inicia o
fluxo **flexível**: a Play pede consentimento e baixa em segundo plano. Quando o download termina, uma faixa
"Atualização pronta · Reiniciar" aparece no lugar das mensagens do shell. Com um cronômetro contando, a faixa não
oferece "Reiniciar". Recusar ou tocar "Agora não" vale só para o processo atual. Nada é persistido.

## Technical Context

- **Language/Version:** Kotlin 2.3.10 (Kotlin integrado do AGP 9.0.1), Compose BOM 2026.02.00.
- **Primary Dependencies:** nova `com.google.android.play:app-update-ktx:2.1.0`, que traz `app-update:2.1.0`. Já está
  no cache Gradle local (usada no IPB). Usa a API `startUpdateFlowForResult(info, ActivityResultLauncher<IntentSenderRequest>,
  AppUpdateOptions)`, que não depende de `onActivityResult`, e a extensão suspensa `requestAppUpdateInfo()`.
- **Storage:** N/A. O estado fica só em memória, sem Room nem DataStore. O schema continua em 7.
- **Testing:** JUnit4 + kotlinx-coroutines-test, com um fake da interface `AppUpdateSource`. Não usa o
  `FakeAppUpdateManager` da Play porque ele exige `Context` (Robolectric/instrumentado).
- **Target Platform:** Android minSdk 24 / targetSdk 36. Só funciona com instalação pela Play.
- **Project Type:** app móvel, módulo único `:app`.
- **Performance Goals:** não atrasar o Hoje (spec027). A checagem começa só depois de `StartupSequence.awaitTodayShown()`.
- **Constraints:** sem novas permissões, sem backend, sem fechar ou bloquear o app.
- **Scale/Scope:** cerca de 6 arquivos novos e 3 alterados.

## Constitution Check

- **I.** A spec 039 tem escopo, exclusões e critérios. A implementação foi pedida pelo usuário em 2026-11-20. ✅
- **II.** Nada muda nos dados: sem schema, sem migração, sem DataStore, e as operações centrais seguem offline. ✅
- **III.** Separação das partes:
  - `core/update/domain` é puro (regras de transição e interface).
  - `core/update/platform` embrulha o `AppUpdateManager`.
  - `core/update/data` tem o controlador `@Singleton`.
  - `features/update` tem o ViewModel e a faixa.

  A dependência nova resolve uma necessidade documentada (FR-001/005). Não há use case de repasse. ✅
- **IV.** A faixa recebe estado e callbacks. Usa as cores do tema e textos em `strings.xml`, é lida pelo TalkBack,
  tem alvos de 48 dp e acompanha a fonte ampliada. Sem permissões e sem logs de conteúdo. ✅
- **V.** Testes JVM cobrem as transições (incluindo recusa, falha e download pendente) e o bloqueio por cronômetro.
  A validação real pela trilha de teste interno da Play fica registrada como não executada. ✅

Reavaliação após o design: sem violações e sem complexidade a justificar.

## Project Structure

```text
specs/039-in-app-update/
├── plan.md  research.md  data-model.md  quickstart.md
├── contracts/app-update-source.md
└── tasks.md (próxima etapa)

app/src/main/java/com/tabitos/app/
├── core/update/domain/AppUpdateModels.kt     # StoreUpdate, AppUpdatePhase, AppUpdateRules (puro)
├── core/update/data/AppUpdateSource.kt       # interface da fonte (usa o launcher do androidx)
├── core/update/platform/PlayAppUpdateSource.kt  # AppUpdateManager → AppUpdateSource
├── core/update/data/AppUpdateController.kt   # @Singleton: fase por processo, checagem única
├── core/update/data/AppUpdateModule.kt       # Hilt: AppUpdateManager + bind da fonte
├── features/update/AppUpdateViewModel.kt     # combina fase + cronômetro → AppUpdateUiState
├── features/update/AppUpdatePresentation.kt  # função pura updateNotice(phase, timerRunning)
├── features/update/AppUpdateStrip.kt         # faixa + efeito do launcher; Preview
└── TabitosApp.kt                             # conecta o ViewModel e põe a faixa no slot snackbar do shell

app/src/test/java/com/tabitos/app/
├── core/update/AppUpdateControllerTest.kt
└── features/update/AppUpdatePresentationTest.kt
```

Alterações: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `res/values/strings.xml`, `TabitosApp.kt`.

## Design

1. **Fonte (`AppUpdateSource`):**
   - `check(): StoreUpdate` devolve NotAvailable, Available, Downloading ou Downloaded. Exceções viram NotAvailable.
   - `installStates(): Flow<StoreUpdate>` é um `callbackFlow` com o listener, removido em `awaitClose`.
   - `startFlexible(launcher): Boolean` usa a última `AppUpdateInfo` guardada.
   - `complete()` instala o download.
2. **Regras puras (`AppUpdateRules`):** recebem a fase atual e um evento e devolvem a nova fase. `DISMISSED` é
   absorvente: na mesma sessão, nada volta a aparecer. Download concluído depois de uma recusa continua sem mostrar
   nada, e a próxima abertura mostra a faixa pelo caminho de download pendente.
3. **Controlador `@Singleton`:**
   - `run()` é chamado pelo ViewModel em `viewModelScope`. A checagem acontece só uma vez por processo (flag).
     Depois disso, `run()` acompanha `installStates()` enquanto o escopo viver.
   - `launchPrompt(launcher)`, `promptResult(accepted)`, `dismiss()` e `install()`.
   - A troca de Activity no mesmo processo não pergunta de novo (FR-007).
4. **ViewModel:** espera `startup.awaitTodayShown()`, roda o controlador e combina `phase` com
   `TimerRepository.observe().map { it.running != null }`. Expõe `StateFlow<AppUpdateUiState(prompt: Boolean,
   notice: UpdateNotice?)>`.
5. **UI:**
   - `rememberLauncherForActivityResult(StartIntentSenderForResult())`.
   - `LaunchedEffect(prompt)` chama `vm.launchPrompt(launcher)`. O launcher é passado na hora e o ViewModel não o
     guarda, então não retém Activity.
   - O resultado `RESULT_OK` chama `promptResult(true)`. Qualquer outro chama `promptResult(false)`.
   - A faixa usa `Snackbar` com `surfaceContainerHighest`, igual à `RescheduledSnackbar`, e não some sozinha.
   - O slot `snackbar` do shell fica com uma `Column` contendo a faixa e a mensagem de remarcação.

## Complexity Tracking

Nenhuma violação.
