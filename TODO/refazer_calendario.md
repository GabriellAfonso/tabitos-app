# Calendário — refazer a tela

Registro de pendência. Não é prompt pronto, não executa Spec Kit nem autoriza implementação.

## Origem

- Nota do usuário no artifact "Mapa do Ethos" (aba Ajustes, tela Calendário), 2026-11-13:
  "muito bagunçado, só salva a informação que vamos refazer isso".
- Decisão: não mexer no Calendário agora; conversar e prototipar antes de refazer.

## Problemas já observados no mapa (estado atual, spec 009)

- Três linhas de filtros (Tipos, Lista, Hábito) antes da grade; no celular a grade fica abaixo da dobra.
- Detalhes do dia ficam embaixo da grade, sem indicação visual de que há mais conteúdo.
- Título do período + Anterior · Ir para hoje · Próximo + Mês | Semana | Dia empilham muitos controles.

## Contexto que muda com os ajustes de 2026-11-13

- A barra inferior foi removida: o Calendário agora abre pelo menu ☰ (Hoje, Calendário, Revisar,
  Recompensas). O redesenho deve considerar esse acesso.

## Pendências para conversar

- Quais filtros continuam e onde ficam (talvez um único botão de filtro).
- Se Mês/Semana/Dia continuam todos.
- Como mostrar o dia escolhido sem empurrar a grade (folha inferior, lista abaixo, outra tela).
- Preservar regras da spec 009: paridade com `inDay`, "Sem dados", acompanhamentos e filtros por lista/hábito.

## Rodada 1 de protótipo (2026-11-17)

- Protótipo: `prototypes/calendario/index.html` (Como está + opções A Folha inferior, B Grade encolhe, C Lista abaixo).
- Uso declarado: principalmente ver tarefas futuras; o Calendário é pouco usado.
- Decidido: manter as visões Mês, Semana e Dia, com controles mais compactos.
- Decidido: filtros (tipo, lista, hábito) num único botão que abre uma folha; filtro ativo vira chip removível.
- Em aberto: onde mostrar o dia tocado (A, B ou C); marcação da célula (pontos, números ou títulos);
  (troca de visão decidida na rodada 2).
- Proposta comum ainda não aprovada: Semana como agenda de 7 dias em lista; botão Hoje só fora do período atual.
- Pendências: tocar tarefa abre detalhes ou conclui direto; mostrar concluídas; visão inicial e memória da
  última visão/filtro; botão + criando tarefa na data escolhida.

## Rodada 2 (2026-11-17)

- Decidido: Mês | Semana | Dia centralizado na barra do topo, no lugar do título "Calendário"
  (☰ à esquerda; Hoje e filtro à direita). Período "‹ Novembro 2026 ›" sozinho e centralizado abaixo.

## Rodada 3 (2026-11-17)

- Descartado: opção A (grade ocupando a tela toda com folha inferior). O usuário não quer o calendário na tela toda.
- Preferência entre B (grade encolhe ao tocar) e C (grade compacta fixa + lista); criada D, mistura das duas:
  grade compacta, tocar só troca a lista, recolher para a semana ao rolar ou pelo botão.
