# Spec 020 — Ajustes do Mapa do Ethos (rodada 1)

Estado: implementado (2026-11-13). Interface e navegação; sem mudança de schema Room nem de regras de domínio.

## Origem

Notas do usuário na aba Ajustes do artifact "Mapa do Ethos" (2026-11-13). Decisões de navegação confirmadas
na conversa: remover página Hábitos e barra inferior; Histórico em Configurações; reordenar listas arrastando
no próprio menu.

## Decisões

- **Sem barra inferior.** Navegação pelo menu ☰: Hoje, Calendário, Revisar pendências, Recompensas (só modo
  jogo), Minhas listas e Configurações (ícone de engrenagem). O ⚙ do topo foi removido de todas as
  telas em 2026-11-20: Configurações abre só pelo menu.
- **Sem página Hábitos.** Hábitos aparecem em Hoje e na aba Hábitos de cada lista; criar continua pelo +.
- **Hábitos excluídos** viram sub-página de Configurações, com o mesmo cartão e detalhes (Restaurar).
- **Histórico de moedas** sai do menu e vira sub-página de Configurações (só com modo jogo). Sub-páginas
  mostram Voltar no lugar do ☰ e voltam para Configurações.
- **Histórico simplificado:** agrupado por dia (Hoje/Ontem/data), uma linha por lançamento com título,
  tipo curto e horário; moedas e XP à direita. Referência da compra e "Relacionado a" saíram da lista.
- **Gerenciar listas removido.** No menu: tocar abre a lista; segurar e arrastar reordena; segurar e soltar
  sem mover abre Editar/Excluir. TalkBack: ação de toque longo "Editar ou excluir lista" e Mover para
  cima/baixo. Caixa de entrada não tem ações. Excluir mantém a confirmação existente.
- **Concluídas da lista** mostram só o que foi resolvido hoje (como em Hoje); o resto fica no histórico.
- **Revisão** perde o texto de dica do topo.
- **Botão +:** "Criar tarefa" (✓, cor primária), "Criar bom hábito" (broto, verde `EthosColors.growth`),
  "Quebrar mau hábito" (sinal de proibido, cor de erro).
- **Dados de demonstração removidos** por completo: tela de debug, repositório, seed, strings e testes.
- **Mau hábito em vermelho:** cartões de evitar/limitar usam `errorContainer`/`error` em vez do âmbar das moedas.
- **Cor Vermelho** acrescentada ao fim da paleta (índice 6, `RED_ITEM_COLOR`); "Quebrar mau hábito" já abre
  com Vermelho e o ícone Proibido.
- **Ícones de hábito novos** (acrescentados ao fim, índices 6–16): Proibido, Bebida, Cigarro, Conteúdo +18,
  Tempo de tela, Fast food, Doces, Apostas, Jogos, Compras, Dormir tarde. Validação usa
  `Habit.COLOR_COUNT`/`ICON_COUNT` e `TaskList.COLOR_COUNT`/`ICON_COUNT`; teste confere com as listas da UI.

## Fora do escopo

- Calendário: será refeito; pendência registrada em `TODO/refazer_calendario.md`.

## Preservação de dados

Nenhuma tabela, coluna ou preferência muda; cores e ícones novos só acrescentam índices. Ordem das listas continua em `TaskAction.OrderLists`; hábitos
excluídos continuam com status `EXCLUDED` e histórico.
