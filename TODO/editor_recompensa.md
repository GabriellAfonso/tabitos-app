# Editor de recompensa (Nova recompensa / Editar)

Protótipo: `prototypes/recompensa-editor/index.html`.

## Decidido

- 2026-11-19: escolhida **A · Folha que cresce**. Acrescentar um lápis no canto do ícone para indicar que dá para
  trocá-lo: bolinha pequena (16 px) no canto superior direito.
- 2026-11-19: implementado (RewardEditor.kt, EditorIconTile em GrowingSheet.kt). O lápis também foi para o
  ícone do editor de hábito.

## Histórico da rodada 1 (2026-11-19)

- Como está: AlertDialog em `RewardsContent.kt` (RewardEditor) com campos de texto para preço, limite e duração,
  e chips de período sempre visíveis.
- A · Folha que cresce (recomendada): mesmo padrão da spec 029 (`GrowingSheet.kt`). Chips Preço / Limite / Uso,
  cada um abre uma folha de escolha; editar abre em tela cheia com linhas rótulo → valor.
- B · Tela em blocos: O que é / Quanto custa / Regras, tudo visível, com referência de preço pela média de
  ganhos dos últimos 7 dias.
- C · Prévia do cartão: cartão da loja ao vivo no topo da folha e linhas embaixo.
- Comum às três: limite e período viram uma regra só ("1× por semana"); duração vira "Na hora" ou "Por tempo".
- Pontos abertos: levar a referência de preço de B para A; mostrar a prévia de C na folha expandida de A;
  atalhos de preço fixos ou calculados.

Nenhuma mudança de schema ou regra proposta. Protótipos não autorizam implementação.
