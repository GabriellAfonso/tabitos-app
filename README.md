# Tabitos

Shell Android implementado: Hoje, Tarefas, Hábitos, Calendário e destinos do menu lateral.
Aparência claro/escuro/sistema salva com DataStore; Compose/Material 3 e Hilt/KSP.
Listas, Caixa de entrada, tarefas, checklists, prazos, recorrências e revisão são persistidos localmente com Room.
Hábitos/metas e moedas/XP/níveis estão implementados, com tabelas editáveis e histórico financeiro local.
Recompensas para resgate continuam como etapa futura, sem dados ou valores fictícios.

## Abrir e compilar

Use Android Studio compatível com AGP 9 (Otter 2025.2.3 ou superior), JDK 17+ (JDK 21 usado
na validação), SDK Platform 36 e Build Tools 36.0.0. Abra a raiz do projeto no Android Studio.
Configure o SDK pelo Android Studio (`local.properties`, ignorado) ou pela variável `ANDROID_HOME`.
Configure `JAVA_HOME` para o JDK. A primeira resolução de dependências precisa de internet.

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Linux/macOS:

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

- Debug instalável: `app/build/outputs/apk/debug/app-debug.apk`.
- Release otimizado sem assinatura: `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Testes JVM verificam datas, recorrências, correções, aparência e recuperação de falhas. Testes Room/Compose
  executam com `:app:connectedDebugAndroidTest` em dispositivo/emulador.

## Instalar e verificar

Com depuração USB autorizada ou emulador iniciado:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.tabitos.app/.MainActivity
```

Android mínimo: 7.0 (API 24). Verifique abertura sem internet, rotação e reabertura,
aparência clara/escura, maior fonte do sistema, retrato/paisagem e rótulos das ações pelo leitor de tela.
A aplicação não solicita permissões nem depende de rede em execução.
Resultados e limitações reais: [verificação atual](specs/005-coins-xp-levels/verification.md).
A fundação tem [evidência histórica](specs/001-android-foundation/verification.md).

## Assinatura local de release

O release usa R8 e redução de recursos; não usa chave debug e não exige chave para compilar sem assinatura.
Para distribuir um APK release, use Android Studio > Build > Generate Signed App Bundle / APK > APK,
selecione release e um keystore privado armazenado fora do repositório. Se precisar criar a chave,
faça isso localmente e mantenha cópia segura; não foi criada uma chave pessoal nesta entrega.
Não use a chave de depuração como identidade de produção. Não versione chaves, senhas ou arquivos de assinatura.
Não há automação de assinatura ou publicação configurada.

## Organização e escopo

`features/shell` contém navegação e apresentações; `features/settings` contém estado e UI de aparência.
`core/preferences` contém o contrato/DataStore e `core/ui/theme` os temas sálvia claro/escuro.
Amostras fictícias existem apenas em previews explícitos; não alimentam o app em produção.
Não há camadas ou dependências futuras instaladas sem uso. Kotlin é integrado ao AGP 9;
o plugin Compose e o classpath Kotlin têm versão alinhada no catálogo central.
As versões são fixas em `gradle/libs.versions.toml`; use o Wrapper incluído.
Documentação da entrega: [spec](specs/001-android-foundation/spec.md),
[plano](specs/001-android-foundation/plan.md), [tarefas](specs/001-android-foundation/tasks.md).

Shell anterior: [spec](specs/002-app-shell/spec.md), [plano](specs/002-app-shell/plan.md).

Entrega de tarefas: [spec](specs/003-task-lists/spec.md), [plano](specs/003-task-lists/plan.md),
[tarefas](specs/003-task-lists/tasks.md), [validação](specs/003-task-lists/quickstart.md).

O domínio compartilhado está em `core/tasks/domain`; o Room em `core/tasks/data` e as telas em `features/tasks`.
O schema inicial é versionado em `app/schemas`. Migrações 1→2→3 preservam os dados; não há fallback destrutivo, lembretes ou resgate de recompensas.
Contabilização em `core/accounting` e interface em `features/accounting`. Documentação:
[spec financeira](specs/005-coins-xp-levels/spec.md), [plano](specs/005-coins-xp-levels/plan.md),
[tarefas](specs/005-coins-xp-levels/tasks.md).
