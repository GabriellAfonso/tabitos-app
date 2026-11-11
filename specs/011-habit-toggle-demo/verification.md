# Verificação — 2026-11-10

## Comportamento
- Prática: alternância explícita e idempotente do estado do dia; remove registros antigos
  duplicados ao desmarcar. Transação existente reconcilia ganhos/estornos, sem modal.
- Hoje: mantém hábitos com meta cumprida visíveis; não mostra barras, progresso do período,
  estado de outros dias ou grupo expansível de metas cumpridas. Ocorrências mostram contagem de hoje.
- Calendário/progresso e correções continuam nos detalhes. Evitar/limitar mantêm confirmação.
- Snackbar com estado no ViewModel oferece Desfazer e Fechar após gravação confirmada.
- Configurações debug: confirmação destrutiva, cancelamento sem escrita, estado de execução/erro/sucesso.
- Demonstração: 4 listas, 6 hábitos (incluindo pausado), tarefas únicas/recorrente/checklist,
  duas semanas anteriores de histórico e revisão, carteira/XP reais, 3 recompensas/1 compra,
  2 metas de tempo e acompanhamento com escala/escolha/texto. Datas relativas ao reset.
- Reset mantém schema 7, tema e permissões; não inicia cronômetro nem ativa lembretes.
  Cancela avisos antigos sob o mesmo lock do coordenador. Bloqueia com cronômetro contando.
  A seleção de lista é limpa quando a lista deixa de existir, evitando filtro antigo após o reset.

## Testes executados
- 115 testes JVM aprovados, incluindo alternância rápida, falha/retry e confirmação/cancelamento do reset.
- API36 Pixel_5: 2 testes Room aprovados (restauração repetida/rollback/bloqueio por cronômetro,
  duplicatas antigas/estorno/idempotência). Repetidos após ajuste de leitura da compra dentro da transação.
- API36 Pixel_5: 3 testes Compose de hábitos aprovados, incluindo Hoje com meta semanal atingida,
  alternância e ausência de barras/progresso de outros dias.
- Build debug, APK de testes e lint executados pelo Gradle Wrapper; relatório em
  `app/build/reports/lint-results-debug.html`.
  Lint: zero erros e 24 avisos (dependências, recursos antigos, ícone/backup e convenções existentes).
- `git diff --check`: sem erros de whitespace.

## Dispositivo e limites
- Celular Xiaomi 2201117TG, Android API33: instalação com `adb install -r` e abertura da Activity.
  Dados atuais preservados: instalar/abrir não carrega a demonstração.
- APK separado de testes bloqueado pelo Android do celular (`INSTALL_FAILED_USER_RESTRICTED`);
  integração e Compose executados no emulador API36. Primeiro arranque frio do emulador teve
  timeout de startup; execução após inicialização terminou normalmente.
- Não validados nesta mudança: API24, R8/release, TalkBack por áudio e restauração manual no celular.
- Sem commit, push, mudança de versão ou migração de schema.

Para iniciar os exemplos no celular: Configurações → Resetar progresso e restaurar demonstração →
Apagar dados e restaurar. A confirmação informa que o conteúdo atual será substituído.

## Correção após relato de crash nas configurações — 2026-11-10

- Log do celular: `Cannot create an instance of class DemoViewModel`, causado por
  `NoSuchMethodException: DemoViewModel.<init> []` ao entrar pela navegação.
- Causa: `viewModel()` dentro da rota usava a fábrica padrão do NavBackStackEntry.
  Criar DemoViewModel no escopo da MainActivity com a fábrica Hilt, como os outros ViewModels,
  e passá-lo obrigatoriamente para DemoSettingsScreen corrige a resolução de dependências.
- Acrescentado SettingsNavigationTest: usa MainActivity, abre configurações, abre a confirmação
  de reset e cancela. Testes anteriores de conteúdo/Room não cobriam a fábrica da rota.
- Atualização instalada com `adb install -r`; abertura da Activity confirmada sem apagar dados.
  Toques remotos no celular são bloqueados por `INJECT_EVENTS`; validação manual solicitada ao usuário.
- Regressão aprovada em API36: `settingsOpensAndDemoResetCanBeCanceledWithoutWriting` (40,358 s).
  Build debug/APK de testes, 115 testes JVM e lint aprovados após a correção.
