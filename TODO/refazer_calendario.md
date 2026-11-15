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
