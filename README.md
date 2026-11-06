# Ethos

Shell Android implementado: Hoje, Tarefas, Hábitos, Calendário e destinos do menu lateral.
Aparência claro/escuro/sistema salva com DataStore; Compose/Material 3 e Hilt/KSP.
Tarefas, hábitos, moedas e recompensas ainda não têm motores ou cadastro; a interface usa estados vazios.

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
- Testes JVM verificam persistência da aparência e recuperação de falhas. Testes Compose de navegação
  executam com `:app:connectedDebugAndroidTest` em dispositivo/emulador.

## Instalar e verificar

Com depuração USB autorizada ou emulador iniciado:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.ethos.app/.MainActivity
```

Android mínimo: 7.0 (API 24). Verifique abertura sem internet, rotação e reabertura,
aparência clara/escura, maior fonte do sistema, retrato/paisagem e rótulos das ações pelo leitor de tela.
A aplicação não solicita permissões nem depende de rede em execução.
Resultados e limitações reais: [verificação atual](specs/002-app-shell/verification.md).
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

Entrega atual: [spec](specs/002-app-shell/spec.md), [plano](specs/002-app-shell/plan.md),
[tarefas](specs/002-app-shell/tasks.md), [validação](specs/002-app-shell/quickstart.md).
