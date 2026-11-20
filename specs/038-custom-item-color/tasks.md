# Tarefas da feature 038 · Cores primárias e cor personalizada

Entrada: [spec](spec.md), [plano](plan.md), [modelo](data-model.md), [contrato](contracts/interface.md).
Testes JVM pedidos pela constituição para regras de negócio e funções puras.

## Fase 1 · Fundação (bloqueia as histórias)

- [X] T001 Criar catálogo puro `ItemColor`/`ItemColors` (paleta ARGB por posição, ordem da linha, `ItemColors.badHabitStart`, decode/encode/isValid/argb, contraste WCAG, `onColorIsLight`, `lowContrastThemes`, HSV ⇄ RGB, `parseHex`, `formatHex`) em app/src/main/java/com/tabitos/app/core/colors/ItemColor.kt
- [X] T002 [P] Teste JVM do catálogo (posições 0–6 válidas, 7 e ARGB não opaco inválidos, encode/decode inversos, contraste conhecido, HSV ida e volta, Hex de 3/6 dígitos com e sem #) em app/src/test/java/com/tabitos/app/core/colors/ItemColorTest.kt
- [X] T003 Validar cor com `ItemColors.isValid` e remover `COLOR_COUNT` em app/src/main/java/com/tabitos/app/core/tasks/domain/TaskRules.kt, TaskModels.kt, app/src/main/java/com/tabitos/app/core/habits/domain/HabitRules.kt e HabitModels.kt
- [X] T004 Funções de desenho `itemColor`, `onItemColor`, `itemColorName` em app/src/main/java/com/tabitos/app/core/ui/components/ItemColorUi.kt

## Fase 2 · US1 Cores primárias numa linha (P1)

- [X] T005 [US1] Trocar `itemColors[...]`/`itemColorNames`/`RED_ITEM_COLOR` por T004/`ItemColors.badHabitStart` em TabitosApp.kt, IconPicker.kt, CalendarContent.kt, HabitDetail.kt, HabitEditor.kt, HabitsContent.kt, HabitsViewModel.kt, StatisticsContent.kt, TaskDetail.kt, TaskDrawer.kt, TasksContent.kt e TrackerPage.kt (app/src/main/java/com/tabitos/app/...)
- [X] T006 [US1] Ícone sobre a cor com `onItemColor` no lugar de branco fixo nos mesmos arquivos (EditorIconTile do hábito recebe `onItemColor`)
- [X] T007 [US1] Nova linha de cores (5 da paleta + Personalizada com lápis para Rosa/Cinza/personalizada) em app/src/main/java/com/tabitos/app/core/ui/components/ColorPicker.kt e nomes em app/src/main/res/values/strings.xml
- [X] T008 [P] [US1] Ajustar teste do mau hábito (começa em `ItemColors.badHabitStart`, tom #D93025) em app/src/test/java/com/tabitos/app/features/habits/HabitsViewModelTest.kt

## Fase 3 · US2 Cor personalizada (P1)

- [X] T009 [US2] Estado puro da folha `CustomColorDraft` e transições (roda, brilho, Hex, RGB, Partir de, modo, resultado) em app/src/main/java/com/tabitos/app/core/ui/components/CustomColorPresentation.kt
- [X] T010 [P] [US2] Teste JVM das transições (Hex inválido mantém a última cor válida e bloqueia, RGB fora de 0–255, Partir de, abrir de Rosa/personalizada) em app/src/test/java/com/tabitos/app/core/ui/components/CustomColorPresentationTest.kt
- [X] T011 [US2] Folha `CustomColorSheet` (roda em Canvas com toque/arraste, Slider de brilho, segmentado Hex | RGB, campos, erro, Partir de, Cancelar/Usar cor) em app/src/main/java/com/tabitos/app/core/ui/components/CustomColorSheet.kt e textos em strings.xml
- [X] T012 [US2] Abrir a folha pelo círculo Personalizada e aplicar o resultado via `onSelect` em app/src/main/java/com/tabitos/app/core/ui/components/ColorPicker.kt
- [X] T013 [P] [US2] Teste JVM das regras: lista e hábito aceitam cor personalizada e recusam inválida em app/src/test/java/com/tabitos/app/core/colors/ItemColorRulesTest.kt

## Fase 4 · US3 Contraste (P2)

- [X] T014 [US3] Prévia nos fundos claro/escuro na folha (aviso de contraste removido depois a pedido do usuário) em app/src/main/java/com/tabitos/app/core/ui/components/CustomColorSheet.kt

## Fase 5 · Acabamento

- [X] T015 Previews com dados explícitos da linha e da folha em ColorPicker.kt e CustomColorSheet.kt
- [X] T016 Rodar testes JVM filtrados (quickstart.md) e registrar em specs/038-custom-item-color/verification.md
- [X] T017 Registrar a feature em AGENTS.md e atualizar TODO/seletor_cor.md

## Dependências

T001→T003/T004→US1→US2 (T012 depende de T007). T009→T011. US3 depende de T011. MVP: Fases 1–2.
Paralelos: T002, T008, T010, T013 (arquivos de teste separados).
