<p align="center">
  <img src="app/src/main/res/drawable-nodpi/tabitos_icon.png" width="96" alt="Ícone do Tabitos">
</p>

<h1 align="center">Tabitos</h1>

<p align="center">
  Tarefas, hábitos e rotinas num só lugar.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/minSdk-24-brightgreen?style=flat" alt="Min SDK 24">
  <img src="https://img.shields.io/badge/licença-GPL--3.0-blue?style=flat" alt="Licença GPL-3.0">
</p>

---

## O que faz

- **Hoje** — tarefas do dia e bons hábitos para marcar com um toque
- **Tarefas e listas** — prazos, recorrências, checklists e revisão do que ficou para trás
- **Hábitos** — bons hábitos para manter e maus hábitos para quebrar, com sequência e constância
- **Cronômetro** — tarefas e hábitos com tempo, que concluem sozinhos ao zerar
- **Calendário** — mês, semana e dia, com acompanhamentos por hábito
- **Lembretes** — avisos por tarefa ou hábito e um lembrete diário de revisão
- **Estatísticas** — resumo e histórico por semana, mês, ano ou tudo
- **Modo jogo** (opcional) — moedas, XP, níveis e recompensas pessoais

## Stack

Kotlin · Jetpack Compose + Material 3 · Hilt (KSP) · Room · DataStore · Coroutines/Flow · AGP 9

## Rodar localmente

Requisitos: Android Studio compatível com AGP 9, JDK 17+ e SDK 36.

```bash
git clone https://github.com/GabriellAfonso/tabitos-app.git
cd tabitos-app
./gradlew :app:assembleDebug        # Windows: .\gradlew.bat :app:assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`. Roda em Android 7.0 (API 24) ou superior.

## Licença

Distribuído sob a [GNU GPL v3](LICENSE).
