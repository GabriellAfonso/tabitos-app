# Loja de recompensas (página Recompensas)

Protótipo: `prototypes/recompensas/index.html`.

## Decidido

- 2026-11-19: escolhida **A · Lista compacta** e implementada (RewardsContent.kt). Tocar na linha abre o editor.

## Histórico da rodada 1 (2026-11-19)

- Como está: `RewardsContent.kt` com saldo solto, botão Nova recompensa e cartões altos com Comprar,
  "Editar <nome>" e "Excluir <nome>" empilhados.
- A · Lista compacta: carteira em cartão âmbar com atalho para o histórico; uma linha por recompensa com o botão
  "◈ preço", motivo quando não dá para comprar, ⋮ com Editar/Excluir e botão flutuante Nova recompensa.
- B · Vitrine em grade: duas colunas; tocar abre folha de detalhes com Comprar/Editar/Excluir.
- C · Por disponibilidade: lista de A em grupos "Dá para comprar agora", "Juntando moedas" (com barra) e
  "Limite atingido"; carteira mostra a próxima recompensa alcançável.
- Pontos abertos: o que o toque na linha abre (editor ou compra); atalho da carteira para o histórico; onde fica
  o aviso "Comprar usa na hora. Não tem devolução."

Editor usado nas opções: folha A de `prototypes/recompensa-editor` (ver `TODO/editor_recompensa.md`).
Nenhuma mudança de schema ou regra proposta. Protótipos não autorizam implementação.
