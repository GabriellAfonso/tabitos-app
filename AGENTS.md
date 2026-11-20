# Tabitos — instruções para agentes

## Projeto
- Estado atual: planejamento, sem app implementado. As escolhas abaixo descrevem a base pretendida.
- Não iniciar implementação até o usuário solicitar a etapa de implementação do Spec Kit.
- App Android de tarefas, hábitos e rotinas, escrito em Kotlin. Nome: Tabitos (antes Ethos; renomeado em 2026-11-19).
- Um módulo `:app`, pacote e applicationId `com.tabitos.app`, interface em Jetpack Compose e Material 3.
  Antes era `com.ethos.app`: Tabitos é instalação nova; a rotina passa do Ethos por backup (spec037).
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

## Qualidade de código
- Código deve ser legível sem o histórico da conversa: nomes que dizem o que é, funções curtas com uma
  responsabilidade, sem efeitos colaterais escondidos. Se precisar de um comentário para explicar *o quê*,
  renomear ou extrair; comentários explicam *por quê* (regra de negócio, armadilha da plataforma).
- Sem números/strings mágicos: usar enums, `const val` ou objetos nomeados. Nunca usar `ordinal` ou
  índices (`filter == 2`) como regra de negócio ou contrato entre camadas.
- Sem condições booleanas aninhadas difíceis de ler: extrair para função/propriedade com nome
  (`showsEmptyState`, `isOverdue`) ou para um estado de tela calculado no ViewModel ou em função pura.
- Composables não calculam regra: ordenação, filtros, agrupamentos e decisões de visibilidade ficam no
  ViewModel ou em funções puras testáveis (`*Presentation.kt`). O Composable só desenha e chama callbacks.
- Nunca usar `LocalDate.now()`/`Instant.now()`/`System.currentTimeMillis()` em Composables ou regras: a
  data vem do relógio injetado (`TaskClock`) via estado; parâmetros padrão não podem ler o relógio.
- Ponto de composição único e plano: `MainActivity` só trata `Intent`s e chama `setContent`; a montagem
  do app fica em `TabitosApp.kt` (pacote raiz), dividida em funções pequenas. Proibido aninhar wrappers
  `XScreen { state -> YScreen { ... } }` para obter estado: coletar com `collectAsStateWithLifecycle()`.
- Listas de parâmetros longas: usar argumentos nomeados nas chamadas e agrupar callbacks relacionados em
  uma `data class` de ações quando passar de ~5 lambdas.
- Sem nomes totalmente qualificados no meio do código (`com.tabitos.app...X(...)`): importar. Evitar
  imports com `*` em arquivos novos.
- Sem código morto, placeholders ("em breve") ou ramos inalcançáveis: remover junto com strings e
  previews que só existiam por eles.
- Não duplicar componentes: antes de criar um, procurar em `core/ui/components`; reaproveitar ou mover para
  lá quando duas features precisarem.
- Reordenação de itens pelo usuário é por arrastar (com alternativa acessível por ações do TalkBack), não
  por botões "Mover para cima/baixo".
- Toda mudança deve deixar o código igual ou melhor do que estava nos arquivos tocados; não copiar padrões
  ruins só porque já existem no arquivo.

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
- Priorizar as skills locais do projeto. Resolver caminhos contra a raiz do projeto.
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
- Não copiar o verde/top bar da IPB: usar os tokens do Tabitos e identificar novas paletas como propostas.
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
  prompt em `TODO/specify_<tema>_app.md`, na raiz do projeto. Isso NÃO executa `$speckit-specify`.
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
- Rotina após mudanças: só testes JVM, filtrando pelo que mudou quando possível (o PC do usuário trava com
  builds pesados). Windows: `.\gradlew.bat :app:testDebugUnitTest --tests "com.tabitos.app.features.tasks.*"`;
  sem filtro quando a mudança for ampla. Linux/macOS: `./gradlew` com os mesmos argumentos.
- Verificação completa (`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`) só quando o usuário pedir;
  ao fim de uma spec (implement/verificação), pedir permissão antes de rodá-la. Sem permissão, registrar build
  debug e lint como não executados.
- Rodar Gradle em segundo plano quando possível; `gradle.properties` limita prioridade, workers e memória para
  manter o PC usável. Não remover esses limites sem pedido.
- Testar regras de negócio, persistência/migrações e falhas relevantes. Preferir fakes.
- Não criar testes que apenas repitam a implementação nem usar retornos Android padrão para esconder falhas.
- Não usar `clean`, `--rerun-tasks` ou `--no-daemon` rotineiramente; diagnosticar o erro primeiro.
- Informar claramente verificações não executadas e limitações do ambiente.
- Testes instrumentados (`connectedDebugAndroidTest`) e emulador só com autorização explícita do usuário
  a cada vez; por padrão rodar apenas testes JVM. Nunca rodar em aparelho físico conectado
  sem autorização. Escrever os testes instrumentados normalmente e registrá-los como não executados.

## Git e publicação
- Conventional Commits quando houver solicitação de commit.
- Não fazer commits, push, tags, publicação ou aumentar versão por iniciativa própria.
- Não copiar configurações pessoais, credenciais, identidade visual ou regras da IPB para este projeto.

## Rotina pessoal em uso
- O usuário já criou sua rotina real no app (informado em 2026-11-11). Tratar os dados existentes como
  dados pessoais em uso, preservando tarefas, hábitos, listas, históricos, configurações e vínculos.
- Antes de mudanças que possam afetar essa rotina, avaliar migrações e impacto no comportamento existente.
  Se houver perda de dados, redefinição ou mudança incompatível inevitável, explicar o impacto concreto e
  combinar a solução com o usuário antes de executar a ação que afeta sua rotina.
- Não restaurar base fictícia, limpar dados, desinstalar o app nem substituir a rotina por dados de teste.
- Os detalhes da rotina não foram fornecidos nem copiados; este registro é uma preferência de preservação,
  não um backup dos dados do app.

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
Spec011: prática marca/desmarca diretamente em Hoje, com estorno (tocar de novo desfaz); sem barras/progresso semanal.
Calendário e progresso nos detalhes; configuração debug restaura base fictícia com confirmação destrutiva.
Restauração Room transacional preserva tema/permissões; bloqueada com cronômetro contando.
Spec012: checkbox de tarefa conclui/desmarca direto; Concluídas recolhida por padrão ao fim, com cartões discretos.
Detalhes/notificações preservam confirmações; recorrência e estornos mantêm regras existentes.
Spec013: editor de tarefa em tela cheia, captura rápida + "Mais opções", seletores de data/hora (sem digitar).
Datas pt-BR únicas (Hoje/Amanhã/Ontem, "10 nov", ano só se diferente); revisão só se vencida e data mudar.
Checklist por itens; repetição em 5 opções mapeando os 8 RepeatKind; sem mudança de schema/regras.
Verificação: specs/013-task-editor-pickers/verification.md; 132 JVM aprovados; instrumentados não executados.
Spec014: hábito não deixa pendência; período fecha sozinho 2 dias após o fim (feito/perdido), sem Revisão.
Hábito perdido não tira moedas; evitar/limitar sem evento = sucesso automático (HabitAction.Settle).
Cartão com sequência e % de constância; editor com frequências simples; "+" com Tarefa/Hábito/Quebrar hábito.
Verificação: specs/014-habits-without-debt/verification.md; 134 JVM aprovados; instrumentados não executados.
Refatoração 2026-11-11: MainActivity só trata Intents; montagem em TabitosApp.kt; shell sem placeholders.
ContentFilter em core/ui/components; seções das páginas de tarefas em função pura (TaskListPresentation).
Spec015: reordenar sempre por arrastar (core/ui/components/Reorder.kt), vizinhos deslizam; ações TalkBack.
Spec016: tarefa/hábito "Com cronômetro" escolhido ao criar (sem checklist); ▶ no cartão; ao zerar conclui sozinho
(desmarcar desfaz), alarme global opcional; concluir manual pede confirmação; cronômetro livre removido.
Constituição: .specify/memory/constitution.md v1.0.0.
Spec017: "Modo jogo" no topo de Configurações (DataStore game_mode, padrão desligado desde 2026-11-19); desligar pede confirmação.
Desligado: esconde Recompensas/Histórico, carteira, preços, dificuldade/estimativa e confirmações financeiras.
Resultados gravados desligado ficam isentos sem lançamento (nunca pagos depois); desfazer pago ainda estorna.
Repositórios leem o modo antes da transação; troca bloqueada com cronômetro de recompensa contando.
Spec018: descartar contagem atual (trecho desde o último ▶) no ⋮ do cartão de Hoje e no diálogo.
Mensagens "Desfazer" de cronômetro iniciado/concluído e hábito marcado removidas (2026-11-18); só Adiada fica,
some sozinha (8 s ajustados pela acessibilidade) e usa cores do tema (surfaceContainerHighest).
Nunca para recompensa/objetivo atingido; ≥1 min pede confirmação; sessão sem tempo ativo é encerrada; auditoria DISCARDED.
Spec019: ícones Material Symbols Rounded como vector drawables (res/drawable/ic_*), sem dependência nova;
índices/chaves de ícones persistidos mantêm a ordem; notificações com ícones próprios; launcher inalterado.
Spec020: sem barra inferior nem página Hábitos; menu ☰ com Hoje/Calendário/Revisar/Recompensas e listas.
Listas no menu: tocar abre, segurar e arrastar reordena, segurar e soltar abre Editar/Excluir; sem Gerenciar listas.
Histórico de moedas e Hábitos excluídos são sub-páginas de Configurações (Voltar); dados de demonstração removidos.
Concluídas da lista só mostram o resolvido hoje; + com Criar tarefa/Criar bom hábito/Quebrar mau hábito.
Mau hábito em vermelho (errorContainer); cor Vermelho e 11 ícones de hábito acrescentados ao fim (índices estáveis).
Spec021: Adiar só no pop-up de detalhes (Amanhã/Escolher data), mantém horário, só esta ocorrência; vencida sem confirmação
(registra prazo perdido); lembretes de data fixa andam os mesmos dias; Desfazer; pop-up "Mínimo" sem histórico nem Fechar.
Adiar grava AuditKind.POSTPONED e desfazer POSTPONE_UNDONE (prazo antigo e data nova), não EDITED (2026-11-17).
Spec022: página Estatísticas no menu ☰ (com e sem modo jogo): abas Resumo/Histórico, período Semana/Mês/Ano/Tudo
(‹ nome ▾ ›, só em memória). Plano/tarefas: specs/022-statistics-history/plan.md, tasks.md (25/25).
Resumo: concluídas, % no prazo, adiadas (POSTPONED), barras; hábitos com HabitRules.successRate por intervalo;
tempo de trechos fechados de tarefas/hábitos; moedas ganhas/gastas e nível. Histórico substitui Histórico de moedas.
Apagar histórico (caixa ao excluir tarefa/hábito, Hábitos excluídos, itens excluídos no Histórico): uma transação,
core/history/data/HistoryErasure; moedas/XP/nível intactos, títulos do razão ficam vazios ("Tarefa/Hábito apagado").
Sem mudança de schema (7). Verificação: specs/022-statistics-history/verification.md; 210 JVM; instrumentados
não executados.
Sessão do cronômetro (Abrir): folha inferior com anel, Pausar/Retomar e + tempo esquecido; Descartar/Encerrar no ⋮;
3 trechos recentes + Ver tudo (Corrigir/Excluir no ⋮ do trecho); sem histórico de alterações nem tempo por período
(auditoria continua gravada; tempo fica em Estatísticas). Presentation: features/timers/TimerSheetPresentation.kt.
Spec023: pop-up de hábito no formato do de tarefa + faixa dos últimos 7 dias (toque marca/desmarca dia aberto; mau
hábito abre folha −/+). Sem campo de data, registros nem períodos; regras/schema inalterados (HabitDetailPresentation).
Spec024: Estatísticas com filtro acima das abas (vale no Resumo e no Histórico): Tudo/Tarefas/Hábitos/Maus hábitos/
Tempo/Moedas. Item (habitId ou série taskId) só pelo próprio item, sem lista: "Ver histórico" nos pop-ups, linha de
hábito no Resumo e aviso de item excluído; chip "Nome ✕" volta ao tipo. Filtro/item/aba só em memória
(StatisticsSelection.kt; navegação via AppRequests.destination).
Spec026: alarme do cronômetro com "Parar alarme" na notificação (também na versão pública) e pop-up no app;
Abrir/deslizar também param. Só silencia (cancela a notificação do canal); conclusão/Desfazer inalterados.
Spec027: abertura rápida — Hoje aparece completo (TodayReadiness) e depois Calendário/Recompensas/Lembretes,
um por vez (core/startup/StartupSequence); abrir a página ou notificação carrega na hora. Refresh de tarefas e
fechamento de hábitos só após o Hoje; tarefas/hábitos gravam só linhas alteradas (core/tasks/data/ChangedRows.kt).
"Fully drawn" no logcat mede o tempo até o Hoje. 243 JVM; instrumentados não executados; medição no celular pendente.
Spec029: editores de tarefa/bom/mau hábito em folha que cresce (core/ui/components/GrowingSheet.kt): criar abre
compacta com chips de valor e teclado; "Mais" ou editar abre tela cheia com linhas rótulo→valor. Sem lembrete no
editor nem troca de tipo; regras/schema inalterados; mapeamento de hábito em HabitEditorPresentation.kt. 262 JVM.
Spec031: Estatísticas com histórico grande — PreparedStatistics (StatisticsComputation.kt) monta uma vez por mudança
de dado/dia/modo jogo; período/filtro/item/"Ver mais" só derivam. Checkpoint do cronômetro ignorado (forStatistics).
HabitRules.periods indexa registros/intervalos (era quadrático); streak/successRate aceitam períodos prontos.
5 anos fictícios (StatisticsDataset): abrir 3,0 s→0,27 s, trocar 2,0 s→3 ms (JVM). Sem mudança de schema. 274 JVM.
Spec032: Remarcar nos detalhes oferece Hoje/Amanhã/calendário e preserva o horário. Antecipações e seus
desfazeres aparecem no Histórico; Adiadas conta POSTPONED. Plano: specs/032-reschedule-task/plan.md.
Spec033: novas tarefas com dia e horário recebem lembrete 30 min antes. Configurações permite também 1 dia
e horário opcional para tarefas só com data. Novas séries herdam a escolha na criação. Plano: specs/033-default-task-reminders/plan.md.
Spec035: prazo "Até uma data" (DeadlineKind.UNTIL_DATE, último do enum) só para tarefa única: início padrão hoje em
RepeatRule.start, fim em fields.date com horário opcional; periodStart = min(início, fim); grupo de período em Hoje.
Remarcar muda só o fim; lembretes padrão como Dia específico (hasDayTime). Sem mudança de schema.
Folha Prazo = opção C de prototypes/prazo: lista com valor à direita + cartão de linhas (PickerStyle.ROW). Plano:
specs/035-deadline-until-date/plan.md.
Spec036: Lembrete de revisão substitui Resumo diário e conserva o horário. Só avisa para tarefas de Hoje ou
bons hábitos disponíveis sem marcação no dia. Toque abre Hoje/Tudo; dispensa encerra o aviso e ele expira à
meia-noite local. Plano: specs/036-daily-review-reminder/plan.md.
Spec037: backup portátil Ethos → Tabitos em Configurações → Dados. Plano: specs/037-portable-backup/plan.md.
<!-- SPECKIT END -->
Spec025: Configurações em lista agrupada (Aparência/Modo jogo/Notificações e alarmes/Dados), linhas em
core/ui/components/SettingsRows.kt; Tema abre diálogo; Ganhos e penalidades é sub-página (EARNINGS, só modo jogo).
Aviso de notificações só quando bloqueadas; resumo diário com seletor de hora; sem mudança de regras/schema.
Spec028: seletor de ícone com uma linha de principais fixos por editor/tipo + "Ver mais" (folha com busca e
categorias); catálogo único core/icons/AppIcon (~127). Índices de lista/hábito e chaves de recompensa preservados:
só acrescentar no fim do enum, nunca renomear (AppIconTest fixa a ordem). Sem mudança de schema.
Spec030: Calendário opção C — Mês|Semana|Dia centralizado (Hoje à esquerda só fora do período, filtro à direita),
"‹ período ›" abaixo, filtros em folha com chips removíveis; Mês = grade compacta fixa com pontos separados
(tarefas/hábitos/acompanhamento) e lista do dia rolando embaixo; Semana = agenda de 7 dias; regras 009 inalteradas.
Hoje (2026-11-18): "Tudo" mostra tarefas e bons hábitos; maus hábitos (evitar/limitar) só no filtro "Hábitos",
para não deixar gatilho à vista (ContentFilter.showsBadHabits). Páginas de lista, lembretes e resumo inalterados.
Spec034: acompanhamento em página cheia (Voltar ao pop-up do hábito) com faixa Hoje e abas Registros/Evolução/
Perguntas; registros por semana (4 + "Ver mais"), dia vazio preenche; data por calendário, sem aaaa-mm-dd. Editor
com cartões expansíveis e barra Adicionar; registro em tela cheia com faixa de escala. Regras/schema 009 inalterados.
Confirmação de moedas (2026-11-18, opção C de prototypes/confirmacao-moedas): diálogo em extrato — título e botão
com a ação (Concluir/Não será feita/Desfazer/Registrar), saldo atual → linhas → novo saldo, "Limite do saldo" no
mínimo, XP com barra do nível; botão vermelho se perde moedas. Função pura core/ui/accounting/FinancialStatement.kt.
Lembretes (2026-11-19, opção A de prototypes/lembretes): pop-up vira folha com linhas (tipo, quando avisa, excluir),
"Novo lembrete" e atalhos Na hora/30 min/1 h/1 dia antes (criam direto; somem se já existem). Escopo Só esta|Esta e
próximas só em tarefa que repete (padrão Só esta). Editor em GrowingSheet com resumo "Avisa…", tipo segmentado, chips,
calendário e dias da semana sem digitar; data escolhida no passado bloqueia, antecedência já passada não.
Textos em features/reminders/ReminderPresentation.kt. Regras/schema inalterados; diálogo Adiar da notificação inalterado.
Recompensas (2026-11-19, opção A de prototypes/recompensas e de prototypes/recompensa-editor): loja em lista
compacta — carteira âmbar com "Histórico" (Estatísticas › Histórico filtro Moedas), linha com botão "◈ preço" ou
motivo (Faltam N / Limite atingido · volta …), ⋮ Editar/Excluir, tocar edita, FAB Nova recompensa. Editor em
GrowingSheet: criar compacto com chips Preço/Limite/Uso, editar em tela cheia com linhas; RewardDraft tipado
(RewardPresentation.kt). EditorIconTile (lápis no canto do ícone) no editor de recompensa e de hábito. Regras/schema
inalterados.
Confirmar compra (2026-11-19, opção C de prototypes/confirmar-compra): sem diálogo — "◈ preço" abre faixa na própria
linha (borda primária) com saldo antes → depois, uso, "Depois desta: N de M", aviso do cronômetro que será pausado
(runningTimer observado no ViewModel; botão "Pausar e comprar") e Voltar/Comprar · ◈ N. Outro preço troca a faixa.
Recibo vira Snackbar que fecha a intenção sozinho (tempo ajustado pela acessibilidade). Diálogo de pausa fica só como
reserva. Funções puras purchaseStrip/receipt/stripIntent em RewardPresentation.kt; regras/schema inalterados.
Página da lista (2026-11-20, opção A de prototypes/listas): barra mostra ícone + nome da lista no lugar de "Tarefas"
(Caixa de entrada sem ícone nem ⋮; listas com ⋮ Editar/Excluir); abas segmentadas "Tarefas N | Hábitos N" sem rótulo
"Lista". Cartão de tarefa (todas as páginas) não mostra "Pendente"; vencida tem meta em vermelho (needsReview).
Sem ⚙ no topo de nenhuma tela: Configurações só pelo menu ☰. Regras/schema inalterados.
Hoje/menu (2026-11-20, opção C de prototypes/hoje e prototypes/menu): cartões com canto único CardShape (16),
ícone do hábito na coluna do checkbox (nome alinhado ao título da tarefa), sem item vazio na lista (header/erro só
quando existem), SectionLabel igual em "Nesta semana", "Hábitos" (Hoje, só com tarefas acima) e "Concluídas".
Menu ☰ com DrawerRow (core/ui/components, 48 dp): só "Tabitos" no topo, Caixa de entrada com ic_inbox, "Criar lista"
como linha, Configurações fixa no rodapé. Destinos, gestos das listas e regras inalterados.
Spec038: cores primárias e cor personalizada em listas/hábitos (core/colors/ItemColor.kt, puro). Campo color Int:
posição 0–6 (PaletteColor.position; nunca mudar) ou ARGB opaco negativo = personalizada; sem schema/migração.
Posições 0/1/2/3/6 viraram Verde/Azul/Roxo/Laranja/Vermelho; Rosa/Cinza (4/5) saem da linha e mostram no círculo
Personalizada. Linha: Vermelho, Laranja, Verde, Azul, Roxo, Personalizada (células dividem a largura). Folha
CustomColorSheet: prévia claro/escuro (sem aviso de contraste), roda Canvas + brilho, Hex | RGB, Partir de. Desenho só por
itemColor/onItemColor (ícone claro/escuro pelo contraste). Plano: specs/038-custom-item-color/plan.md.
