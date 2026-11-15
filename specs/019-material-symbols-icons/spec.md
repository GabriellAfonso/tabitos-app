# Spec 019 — Ícones Material Symbols

Estado: implementado (2026-11-13). Mudança só de interface; sem alteração de schema, regras ou dados.

## Motivo

Os ícones do app eram caracteres Unicode desenhados como texto (`☰ ◉ ◇ ▦ ⚙ ⋮ ✎ ⠿ ▶ ❚❚`, emojis nas
recompensas). O desenho variava com a fonte do aparelho, não seguia o peso do Material 3 e havia símbolos
repetidos (Hábitos e Recompensas usavam o mesmo `◇`). As notificações usavam ícones genéricos do sistema.

## Decisão

- Usar **Material Symbols Rounded**, peso 400, importados como vector drawables em `res/drawable/ic_*.xml`
  (pacote npm `@material-symbols/svg-400@0.49.0`, licença Apache 2.0). Sem nova dependência Gradle;
  `material-icons-extended` foi descartado por estar congelado e ser pesado.
- A barra inferior com variantes preenchidas foi removida depois pela spec 020; os `*_filled` saíram junto.
- Gaveta passa a ter ícone em cada destino e em Configurações.
- Ícones da notificação: `ic_notification_reminder` (lembretes) e `ic_notification_timer` (cronômetro),
  brancos, como o Android exige para o ícone pequeno.

## Mapeamento

| Uso | Antes | Agora |
|---|---|---|
| Hoje / Tarefas / Hábitos / Calendário | `◉ ☰ ◇ ▦` | `today`, `checklist`, `self_improvement`, `calendar_month` |
| Revisão / Recompensas / Histórico / Configurações | `◷ ◇ ↔ ⚙` | `pending_actions`, `redeem`, `history`, `settings` |
| Menu, criar, editar, mais ações | `☰ + ✎ ⋮` | `menu`, `add`, `edit`, `more_vert` |
| Arrastar, remover, fechar, marcar, seta | `⠿ − ✕/× ✓ ›` | `drag_indicator`, `remove`, `close`, `check`, `chevron_right` |
| Cronômetro no cartão | `▶ / ❚❚` | `play_arrow` / `pause` |
| Mais opções do editor | `▴ / ▾` | `keyboard_arrow_up` / `keyboard_arrow_down` |
| Tarefa recorrente | desenho próprio `ic_task_repeat` | `repeat` |
| Ícones de lista (índice 0–5) | `☰ ▣ ⌂ ▤ ♡ ☆` | `format_list_bulleted`, `work`, `home`, `menu_book`, `favorite`, `star` |
| Ícones de hábito (índice 0–5) | `❧ ▤ ⌂ ▣ ♡ ☆` | `eco`, `menu_book`, `home`, `work`, `favorite`, `star` |
| Recompensas (`GIFT/GAME/FOOD/REST`) | `🎁 🎮 🍎 ☕` | `redeem`, `sports_esports`, `restaurant`, `coffee` |

## Preservação de dados

`TaskList.icon` e `Habit.icon` continuam sendo o índice persistido e `Reward.icon` continua sendo a chave.
A ordem das listas de opções foi mantida, então cada item existente mostra o novo desenho do mesmo ícone.
Novas opções, se surgirem, entram apenas no fim da lista.

## Fora do escopo

- Ícone do launcher (`ethos_icon.png`, `launcher_foreground.xml`).
- Glifo da moeda `◈` (decisão da spec 010, também usado nos textos) e marcas `✓ ✕ •` junto ao número do dia
  no calendário de hábitos.
