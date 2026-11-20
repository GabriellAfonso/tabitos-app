# Plano de implementação da feature 038

Branch de trabalho `master` · 2026-11-20 · [Especificação](spec.md)

## Abordagem

A cor continua no mesmo campo `Int` de `TaskList.color` e `Habit.color` (colunas `color INTEGER NOT NULL`, schema 7
inalterado). Valores 0–6 são posições da paleta; um ARGB opaco (`0xFFRRGGBB`, sempre negativo como `Int`) é cor
personalizada. Qualquer outro valor é inválido. Itens existentes não são regravados: só muda o tom que as posições
0, 1, 2, 3 e 6 desenham.

Um catálogo puro em Kotlin, `core/colors/ItemColor.kt` (sem Android/Compose, como `core/icons/AppIcon.kt`), concentra
paleta, codificação, validação, contraste, HSV e leitura de Hex/RGB. A UI troca todos os `itemColors[x]` por uma
única função de desenho e o `ColorPicker` passa a mostrar a linha nova e o círculo Personalizada, que abre a folha
`CustomColorSheet` (roda em Canvas, `Slider` de brilho, segmentado Hex | RGB, "Partir de", prévia nos dois temas e
sem aviso de contraste). O estado em edição da folha é efêmero da UI (`rememberSaveable`). As transições ficam em
funções puras testáveis (`CustomColorPresentation.kt`); só o resultado de "Usar cor" entra no rascunho do editor,
que já pertence ao ViewModel.

## Contexto técnico e organização

Android, Kotlin, módulo `:app`, Compose Material 3, Hilt e Room 2.8.5 (schema 7, sem migração). Nenhuma dependência
nova.

- `core/colors/ItemColor.kt` (novo, puro): `ItemColor` (`Palette(index)` | `Custom(rgb)`), `ItemColors.decode/encode/
  isValid`, paleta ARGB com nomes por chave, ordem da linha, `ItemColors.badHabitStart`, contraste WCAG, `onColorIsLight`,
  `lowContrastThemes`, HSV ⇄ RGB, `parseHex`, `formatHex`.
- Domínio: `TaskRules` e `HabitRules` validam com `ItemColors.isValid`; `TaskList.COLOR_COUNT`/`Habit.COLOR_COUNT`
  saem (eram só para a validação e um teste).
- `core/ui/components/ItemColorUi.kt` (novo): `itemColor(stored): Color`, `onItemColor(stored): Color`,
  `itemColorName(stored)`. Substitui `itemColors`/`itemColorNames` em TabitosApp, IconPicker, Calendar,
  HabitDetail, HabitEditor, HabitsContent, Statistics, TaskDetail, TaskDrawer, TasksContent e TrackerPage.
- `core/ui/components/ColorPicker.kt`: linha Vermelho/Laranja/Verde/Azul/Roxo + Personalizada; Rosa/Cinza e cores
  personalizadas aparecem no círculo Personalizada com lápis.
- `core/ui/components/CustomColorSheet.kt` e `CustomColorPresentation.kt` (novos): folha e estado puro.
- `EditorIconTile` e os demais ícones sobre a cor usam `onItemColor` no lugar de `Color.White`.
- `HabitsViewModel` passa a usar `ItemColors.badHabitStart` (antes `RED_ITEM_COLOR`).
- Strings: nomes Verde/Azul/Roxo/Laranja/Vermelho, Rosa/Cinza mantidos, textos da folha.

## Constitution Check

Antes e após o desenho:

- I: spec 038 com critérios; implementação pedida pelo usuário em 2026-11-20 ("rodar specify, plan, tasks e
  implement").
- II: sem mudança de schema nem migração; dados existentes valem como antes (posição 0–6). Backup copia o campo sem
  mudança. Offline.
- III: `core/colors` sem Android; features não se importam; nenhuma camada ou dependência nova além de um catálogo
  compartilhado que substitui listas espalhadas.
- IV: textos em `strings.xml`; alternativa acessível à roda (campos, "Partir de", `Slider`); nomes falados; alvos
  48 dp; folha rolável; tema claro/escuro.
- V: testes JVM de codificação/validação, contraste, HSV/Hex/RGB e transições da folha; regras de lista/hábito com
  cor personalizada; instrumentados não executados por padrão.

## Entrega

Consultar [decisões](research.md), [modelo](data-model.md), [contrato de interface](contracts/interface.md) e
[roteiro de verificação](quickstart.md).
