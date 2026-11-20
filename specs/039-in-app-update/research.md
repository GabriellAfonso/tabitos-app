# Research — 039 Aviso de atualização

## Modo de atualização

- **Decision:** usar só `AppUpdateType.FLEXIBLE`.
- **Rationale:** o usuário decidiu assim em 2026-11-20. O modo imediato bloqueia o app e reinicia no meio de um
  cronômetro.
- **Alternatives:** `IMMEDIATE` como no IPB, com `finish()` se a pessoa cancelar. Também foi considerado usar o
  imediato só para alta prioridade (`updatePriority`). As duas opções foram rejeitadas.

## Biblioteca

- **Decision:** `com.google.android.play:app-update-ktx:2.1.0`.
- **Rationale:** é a última versão estável conhecida e já está no cache Gradle local. A API base 2.1.0 tem
  `startUpdateFlowForResult(AppUpdateInfo, ActivityResultLauncher<IntentSenderRequest>, AppUpdateOptions)`,
  verificado com `javap`. A extensão `requestAppUpdateInfo()` do ktx evita converter `Task` à mão.
- **Alternatives:**
  - `app-update` sem ktx, adaptando o `Task` com `suspendCancellableCoroutine`. Exigiria código extra.
  - `requestUpdateFlow()` do ktx. Não foi escolhido porque o `Available` dele só inicia o fluxo por `Activity` e
    `requestCode` (API antiga).

## Onde iniciar o fluxo

- **Decision:** o `ActivityResultLauncher` fica na composição. O ViewModel recebe o launcher como argumento de uma
  chamada e não o guarda.
- **Rationale:** AGENTS.md proíbe Activity/Context no ViewModel e o uso de `onActivityResult`. O `MainActivity` deve
  só tratar Intents.
- **Alternatives:** registrar o launcher no `MainActivity`. Isso violaria a regra do ponto de composição único.

## "Mesma abertura"

- **Decision:** controlador `@Singleton` com a flag `checked` e a fase em memória. Vale enquanto o processo viver.
- **Rationale:** rotação, troca de tema e voltar do segundo plano não são novas aberturas. Um processo novo é.
- **Alternatives:**
  - Estado só no ViewModel. Perderia a fase se a Activity fosse destruída com o processo ainda vivo.
  - DataStore com uma data de adiamento. Foi rejeitado na decisão 4 (sem adiamento persistido).

## Cronômetro

- **Decision:** observar `TimerRepository.observe()` e usar `running != null`, o mesmo padrão do `RewardsViewModel`.
  Isso vale para qualquer cronômetro contando, inclusive de recompensa.
- **Rationale:** `completeUpdate()` reinicia o processo, e o cronômetro voltaria pausado no checkpoint (spec008).

## Testes

- **Decision:** fake próprio de `AppUpdateSource` em testes JVM.
- **Rationale:** o `FakeAppUpdateManager` da Play precisa de `Context`, e o projeto não usa Robolectric. O fluxo real
  só pode ser validado pela trilha de teste interno da Play.
