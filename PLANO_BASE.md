# Plano base — Ethos

## Estado e objetivo

Projeto em planejamento. Nenhum app está implementado. Construir um app Android de tarefas, hábitos e
rotinas inspirado no TickTick. O escopo funcional detalhado será definido pelo Spec Kit.

## Decisões aceitas

- Kotlin, Jetpack Compose e Material 3.
- Gradle Kotlin DSL, catálogo de versões e Gradle Wrapper.
- Hilt e KSP para injeção e geração de código.
- Room para dados estruturados locais; DataStore para preferências.
- Coroutines, Flow, ViewModel e StateFlow para operações e estado da UI.
- Navigation Compose e uma única Activity.
- Organização por funcionalidades, com infraestrutura compartilhada em `core/`.
- Tema claro, escuro e sistema; interface em português.
- Funcionamento offline desde o início. Backend, autenticação e sincronização ficam para decisão futura.
- Debug/release separados; R8 na versão release.

## Propostas a validar no planejamento técnico

- Nome Ethos, pacote `com.ethos.app` e módulo único `:app`.
- minSdk 24; compileSdk/targetSdk e versões das dependências escolhidos pela compatibilidade no momento
  da configuração. Evitar copiar opções antigas do Gradle da referência.
- Áreas iniciais: Hoje, Hábitos, Calendário e Configurações. Fluxos e navegação ainda não especificados.
- Use cases somente quando houver regras de negócio que justifiquem a camada.
- JUnit e kotlinx-coroutines-test para regras e estado; testes instrumentados Room para persistência
  e migrações quando existirem schemas.

## Sequência com Spec Kit

1. Integração preparada: Specify CLI existente e skills locais do Codex em `.agents/skills/`, com
   templates e scripts PowerShell em `.specify/`. Nenhum código Android gerado.
2. Constituição v1.0.0 definida em `.specify/memory/constitution.md` em 2026-11-06:
   arquitetura, qualidade, dados locais, acessibilidade, privacidade e execução por etapa.
3. Especificar a primeira feature: fundação técnica do app, com escopo e critérios de aceite claros.
4. Esclarecer decisões pendentes, incluindo plataformas e versão mínima do Android.
5. Gerar o plano técnico e validar versões e compatibilidade das ferramentas.
6. Gerar tarefas e analisar consistência entre spec, plano e tarefas.
7. Implementar somente quando solicitado pelo usuário.

Não executar automaticamente todas as etapas. Cada documento deve distinguir decisões aceitas,
propostas e funcionalidades futuras.

## Primeira implementação futura

Após a especificação e autorização da etapa de implementação: criar o projeto, configurar ferramentas,
estabelecer tema e navegação mínima, integrar persistência e verificar build/lint/testes pertinentes.
Não incluir CRUD completo, recorrências, lembretes, login ou sincronização nessa fundação sem especificá-los.

## Extensões funcionais registradas durante o design

- Lembretes e notificações fazem parte do produto desejado, em specify próprio.
- Acompanhamento de tempo iniciado pelo usuário para tarefas/atividades e metas por período
  (ex.: 30 minutos de leitura ou 3 horas semanais).
- Recompensas com duração também poderão ter acompanhamento de tempo (ex.: 2 horas de gameplay).
- Regras, ordem das entregas e integração com moedas/XP ainda serão especificadas.
- Registro detalhado: `TODO/planejamento_lembretes_tempo.md`.

## Referência

`C:/Users/gabri/Projetos/Ipb_castelo_branco/app`: aproveitar convenções de arquitetura e desenvolvimento.
Não copiar credenciais, configurações pessoais, domínios, marca ou regras funcionais da IPB.
