# Ethos — instruções para agentes

## Projeto
- Estado atual: planejamento, sem app implementado. As escolhas abaixo descrevem a base pretendida.
- Não iniciar implementação até o usuário solicitar a etapa de implementação do Spec Kit.
- App Android de tarefas, hábitos e rotinas, escrito em Kotlin. Nome inicial: Ethos.
- Um módulo `:app`, pacote inicial `com.ethos.app`, interface em Jetpack Compose e Material 3.
- Gradle Kotlin DSL e catálogo em `gradle/libs.versions.toml`. AGP 9 com Kotlin integrado; não aplicar
  `org.jetbrains.kotlin.android` nem desativar `android.builtInKotlin`/`android.newDsl`.
- Não introduzir backend, login, Firebase ou sincronização sem uma necessidade definida.

## Arquitetura
- Uma Activity (`MainActivity`), com navegação Compose.
- Organização por funcionalidades em `features/`; infraestrutura compartilhada em `core/`.
- Fluxo: UI → ViewModel → Repository (interface) → implementação → Room/DataStore.
- Use cases quando encapsularem regras de negócio relevantes; evitar camadas que só repassam chamadas.
- Features não importam implementações de outras features. Compartilhar contratos pelo `core` quando necessário.
- `domain/` não depende de Android, Compose, Room ou ViewModel.
- Room é a fonte de verdade dos dados estruturados; DataStore guarda preferências.
- Exportar e versionar schemas Room. Mudanças de schema exigem migração; não usar fallback destrutivo.
- Não usar caches JSON como substituto do banco de tarefas.

## UI e estado
- ViewModel expõe `StateFlow<UiState>` imutável; coletar com `collectAsStateWithLifecycle()`.
- Separar a Screen que conecta o ViewModel do conteúdo que recebe estado e callbacks.
- Composables não acessam banco/repositório e não contêm regras de negócio.
- Representar carregamento, vazio, conteúdo e erro quando aplicáveis; não inventar estados desnecessários.
- Usar `viewModelScope`; nunca `GlobalScope`. Não guardar Activity/Context no ViewModel.
- Efeitos transitórios não podem carregar resultados críticos que se perdem sem coletor; preferir estado
  com confirmação de consumo para mensagens/ações que precisam sobreviver à recriação.
- Componentes compartilhados devem ter Preview com dados explícitos.
- Tema claro/escuro/sistema via DataStore, sem reiniciar a Activity.
- Código e identificadores em inglês; textos visíveis em português em `strings.xml`.
- Considerar acessibilidade, descrição de ícones e escalonamento de fonte.

## Dados e segurança
- Hilt para injeção; KSP para Hilt e Room; Coroutines/Flow para assincronismo.
- Nunca gravar segredos ou keystores no Git. `BuildConfig` não protege segredos de um APK.
- Não registrar conteúdo de tarefas ou dados pessoais em logs.
- Não pedir permissões antes de existir uma funcionalidade que as justifique.
- WorkManager serve para trabalho adiável; lembretes com horário preciso exigem projeto próprio.
- Datas, recorrências e fusos precisam de regras explícitas antes de implementar agendamento.

## Documentação e alterações
- Usar Spec Kit: constitution → specify → clarify (quando necessário) → plan → tasks → analyze → implement.
- Executar uma etapa por vez conforme solicitado. Gerar plano ou tarefas não autoriza implementar.
- O plano base em `PLANO_BASE.md` é um registro inicial, não uma spec/plan gerada pelo Spec Kit.
- Constituição oficial: `.specify/memory/constitution.md`; specs de features em `specs/`.
- Integração local em `.agents/skills/`, infraestrutura em `.specify/`, scripts PowerShell.
- Priorizar as skills locais do projeto. Resolver caminhos contra a raiz do Ethos.
- Hooks e scripts do Spec Kit não autorizam commits, publicação ou implementação por si só.
- Atualizações automáticas de contexto devem alterar apenas o bloco SPECKIT deste arquivo.
- Ler a documentação pertinente em `specs/` antes de mudar comportamento.
- Para novas funcionalidades, registrar escopo e regras em `specs/<feature>/spec.md` antes da implementação.
- Atualizar documentação quando o comportamento ou decisões mudarem; correções triviais não exigem ritual.
- Distinguir o que está implementado de ideias futuras. Não inventar requisitos para preencher specs.
- Não criar abstrações, módulos ou dependências sem necessidade concreta.
- Kotlin official, limite de 120 caracteres por linha.

## Protótipos para discutir interface
- Usar HTML local em `prototypes/<tema>/index.html`, mantendo o mesmo arquivo nas rodadas seguintes.
- Mostrar mockups de celular de aproximadamente 380 px lado a lado, com barra de status,
  rolagem interna, áreas seguras e navegação coerentes com o app Android.
- Ler o código e tema atuais antes de representar "Como está"; manter comparação com a tela real.
- Não copiar o verde/top bar da IPB: usar os tokens do Ethos e identificar novas paletas como propostas.
- Oferecer claro/escuro e "Igual à página", controles interativos e dados fictícios realistas.
- Sob cada opção, explicar mudanças, vantagens e impacto previsto. Textos em português.
- Após escolha do usuário, preservar decisões aprovadas e variar apenas os pontos em discussão.
- Protótipos são instrumentos de planejamento: não alteram o app Kotlin, não salvam dados pessoais
  e não tornam regras de negócio aprovadas. Registrar decisões e pendências em `TODO/`.
- Preferência adaptada de `feedback-app-design-artifacts.md` na memória do Claude do projeto IPB.
- Para abrir protótipos, preferir Brave com o perfil pessoal do usuário; verificar o nome/diretório
  do perfil sem expor outros dados do navegador. Não afirmar que a janela ficou visível sem confirmação.

## Salvar prompts de specify para outro chat
- Quando o usuário pedir para guardar/gerar um specify após uma conversa de design, salvar um
  prompt em `TODO/specify_<tema>_app.md`, na raiz do Ethos. Isso NÃO executa `$speckit-specify`.
- Conversar sobre os pontos pendentes antes de consolidar o prompt. Não tratar sugestões como decisões.
- Cabeçalho em português: título, estado (rascunho/pronto), instrução para executar `$speckit-specify`
  com o corpo após `---` e referências aos documentos pertinentes.
- Corpo em inglês, autocontido e detalhado: estado atual verificado do projeto, objetivo, escopo,
  todas as decisões tomadas e seus motivos, exclusões, critérios de aceite e questões abertas.
- Registrar a origem da conversa (ID/link/caminho de transcript) apenas se estiver disponível;
  nunca inventar identificadores nem depender exclusivamente de um transcript para transmitir decisões.
- Se não houver transcript acessível, registrar esse fato e incluir as decisões completas no arquivo.
- Para trabalhos futuros com backend, criar um arquivo por lado e cruzar referências quando aplicável.
- O chat executor deve ler o prompt, constituição, AGENTS.md e referências antes de perguntar;
  consultar a conversa de origem se acessível e perguntar apenas o que ainda não foi decidido.
- Salvar o prompt não autoriza especificar, planejar, implementar ou fazer commits automaticamente.

## Verificação
- Os comandos abaixo só se aplicam depois que o app e o Gradle Wrapper forem criados.
- Windows: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Linux/macOS: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Testar regras de negócio, persistência/migrações e falhas relevantes. Preferir fakes.
- Não criar testes que apenas repitam a implementação nem usar retornos Android padrão para esconder falhas.
- Não usar `clean`, `--rerun-tasks` ou `--no-daemon` rotineiramente; diagnosticar o erro primeiro.
- Informar claramente verificações não executadas e limitações do ambiente.

## Git e publicação
- Conventional Commits quando houver solicitação de commit.
- Não fazer commits, push, tags, publicação ou aumentar versão por iniciativa própria.
- Não copiar configurações pessoais, credenciais, identidade visual ou regras da IPB para este projeto.

<!-- SPECKIT START -->
Fundação Android implementada: specs/001-android-foundation/spec.md.
Interface/navegação implementada: specs/002-app-shell/spec.md.
Listas/tarefas/checklists/prazos/recorrências/revisão implementados: specs/003-task-lists/spec.md.
Hábitos/metas implementados: specs/004-habits-goals/spec.md.
Moedas/XP/níveis implementados: specs/005-coins-xp-levels/spec.md.
Recompensas pessoais implementadas: specs/006-personal-rewards/spec.md.
Lembretes/notificações implementados: specs/007-reminders-notifications/spec.md.
Plano: specs/007-reminders-notifications/plan.md; tarefas: specs/007-reminders-notifications/tasks.md (20/20).
Verificação: specs/007-reminders-notifications/verification.md (2026-11-07).
Debug/lint/release R8, 85 testes JVM e 44 testes Android aprovados em API36; sem novos avisos lint.
API24, dispositivo físico e TalkBack por áudio não validados; fabricante/force-stop não contornáveis.
Room2.8.5 com schemas1–5 exportados; migração preservadora4→5 validada após cadeia1→4.
Lembretes opcionais múltiplos por tarefa/hábito, escopo de série, data/antecedência/dias da semana.
Alarmes locais únicos com fallback aproximado sem acesso exato; adiar substitui o mesmo slot.
Ações abrem a Activity existente; Concluir revalida e usa confirmações existentes; hábito nunca registra.
Entrega/dispensa/recuperação não geram resultado de domínio; recuperação sem rajada de pendências.
Resumo diário desligado por padrão, horário escolhido, contagens reais de Hoje/revisão.
Permissão de notificação pedida em contexto após configurar; status de canal/acesso exato explicados.
Carteira, XP/níveis, recompensas e históricos mantêm as regras das features 005/006.
DataStore preserva temas; configurações de agendamento ficam no Room (schema5).
Cronômetros/metas de tempo/recompensas temporizadas implementados: specs/008-timers-duration-goals/spec.md.
Plano: specs/008-timers-duration-goals/plan.md; tarefas: specs/008-timers-duration-goals/tasks.md (20/20).
Verificação: specs/008-timers-duration-goals/verification.md; 93 JVM e 52 Android aprovados em API36.
Room schema6 (timer_goals/sessions/segments/audits, reward_minutes); migração preservadora5→6 testada.
Sessões ligadas a tarefa/hábito/compra; um cronômetro contando; tempo por relógio monotônico e checkpoints.
Reinício recupera último checkpoint pausado; meta pausa no objetivo e só conclui via fluxos existentes.
Serviço em primeiro plano specialUse com notificação privada Pausar/Abrir; sem alarme exato/isenção.
Compra temporizada: consentir pausa antes de cobrar; débito, recibo e sessão na mesma transação.
Calendário mês/semana/dia e acompanhamentos por hábito implementados: specs/009-calendar-habit-trackers/spec.md.
Plano: specs/009-calendar-habit-trackers/plan.md; tarefas: specs/009-calendar-habit-trackers/tasks.md (30/30).
Verificação: specs/009-calendar-habit-trackers/verification.md; 108 JVM e 58 Android aprovados em API36.
Room schema7 (trackers, versões, perguntas, opções, registros, respostas); migração preservadora6→7 testada.
Formulário versionado; registros guardam versão, data de referência e momento; um por dia via índice único.
Acompanhamentos nunca alteram moedas/XP/metas/registros do hábito; arquivar substitui remoção destrutiva.
Lembrete ReminderKind.TRACKER: aviso neutro contínuo (dispensável no Android 14+), Preencher/Adiar, só do dia.
Calendário substitui busca por data: filtros tipo/lista/hábito, "Sem dados", paridade com inDay das tarefas.
Paleta Ameixa e modelo B de Hoje implementados: specs/010-ameixa-visual-refresh/spec.md (só interface).
Moedas em âmbar (tertiary) com ◈; ocorrências evitar/limitar mantêm confirmação financeira.
Spec011: prática marca/desmarca diretamente em Hoje, com estorno e Desfazer; sem barras/progresso semanal.
Calendário e progresso nos detalhes; configuração debug restaura base fictícia com confirmação destrutiva.
Restauração Room transacional preserva tema/permissões; bloqueada com cronômetro contando.
Spec012: checkbox de tarefa conclui/desmarca direto; Concluídas expandida ao fim, com cartões discretos.
Detalhes/notificações preservam confirmações; recorrência e estornos mantêm regras existentes.
Constituição: .specify/memory/constitution.md v1.0.0.
<!-- SPECKIT END -->
