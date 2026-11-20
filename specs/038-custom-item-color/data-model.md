# Modelo de dados da feature 038

Sem mudança de schema (Room 7). Campos afetados: `task_lists.color` e `habits.color` (`INTEGER NOT NULL`).

## Valor gravado

| Valor | Significado |
|---|---|
| 0–6 | Posição da paleta (abaixo) |
| `0xFF000000 or rgb` (Int negativo) | Cor personalizada opaca `#RRGGBB` |
| qualquer outro | Inválido: regras de lista/hábito recusam |

## Paleta (posições nunca reordenadas)

| Posição | Nome | Tom | Na linha |
|---|---|---|---|
| 0 | Verde (era Sálvia) | #2E8B3E | 3º |
| 1 | Azul | #1F6FD6 | 4º |
| 2 | Roxo | #9254DE | 5º |
| 3 | Laranja | #DB6400 | 2º |
| 4 | Rosa | #A04D68 | não |
| 5 | Cinza | #657078 | não |
| 6 | Vermelho | #D93025 | 1º (início do mau hábito) |

## Tipo

- `ItemColor.Palette(index)` | `ItemColor.Custom(rgb: Int /* 0xRRGGBB */)`.
- `ItemColors.decode(stored): ItemColor?` (null se inválido) e `encode(color): Int` são inversas para todo valor
  válido.
- `argb(stored)`: tom opaco a desenhar; valor inválido cai na posição 0 só para desenho (a regra recusa ao salvar).

## Estado da folha (efêmero)

`CustomColorDraft(hue 0–360, saturation 0–1, value 0–1, mode HEX|RGB, hexText, rgbTexts[3], invalid: Boolean)`.
Transições: `fromStored`, `withWheel(h, s)`, `withValue(v)`, `withHexText`, `withRgbText(i, text)`, `withStart(index)`,
`withMode`; `result(): Int?` (null quando inválido).
