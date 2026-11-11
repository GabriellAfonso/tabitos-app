# Paleta do tema — estudo visual

Registro de conversa. Não executa Spec Kit nem autoriza implementação.

## Pedido

- Em 2026-11-07, depois de instalar o app no celular, o usuário disse que não gostou do verde atual e pediu
  opções melhores, com tema claro e escuro, no formato de artifact usado no app da IPB.

## Rodada 1 (`prototypes/tema/index.html`)

- Artifact: https://claude.ai/artifact/3XTcoJc6JLKx3Yv5EGW5ED
- "Como está": verde sálvia atual (`core/ui/theme/EthosTheme.kt`).
- Propostas: A Índigo, B Ameixa, C Petróleo, D Grafite e âmbar. Cada uma traz esquemas claro e escuro.
- Impacto previsto: trocar só os esquemas de cor em `EthosTheme.kt`; as cores de hábitos e listas continuam.

## Rodada 2 (mesmo arquivo e mesmo artifact)

- Escolha do usuário: paleta B · Ameixa (claro e escuro).
- O usuário pediu para aplicar o design do protótipo do Codex (`prototypes/hoje/index.html`, modelo B): carteira
  com o símbolo de moeda ◈, aviso de revisão, filtros segmentados, tarefas com caixa e cartões de hábito.
- A rodada 2 mostra: "Como está" (layout atual com Ameixa) e o modelo B com Ameixa em Hoje, Calendário e Recompensas.
- A moeda usa âmbar (#93601A no claro, #E8BE7A no escuro) para se separar do roxo dos destaques.

## Decisão e implementação

- 2026-11-07: o usuário pediu paleta e modelo B juntos. Implementado em `specs/010-ameixa-visual-refresh/spec.md`.

## Pendências

- Ainda não decidido: se as cores de hábitos e listas acompanham a nova paleta.
