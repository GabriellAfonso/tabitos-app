# Detalhes da tarefa e Adiar — planejamento

Estado: implementado na spec 021 (2026-11-16): `specs/021-task-postpone-detail-popup/`.

Protótipos:
- `prototypes/adiar-tarefa/index.html` — fluxo do Adiar (menu do cartão, folha, seletor, vencidas, Desfazer).
- `prototypes/detalhes-tarefa/index.html` — redesenho do pop-up de detalhes da tarefa.

## Decisões tomadas
- Detalhes da tarefa continuam um pop-up central (AlertDialog), mais polido e menos bagunçado.
  Rejeitadas: folha inferior e tela cheia.
- Base escolhida: "versão 1 · ícones embaixo" — resumo numa linha (prazo, lista, repetição, lembretes),
  Concluir em destaque, Adiar e Não será feita como botões, fileira de ícones Editar/Lembretes/Lista/Excluir.
- Rodada 3: escolhido o "mínimo" (2026-11-16). Título; linha de resumo (prazo, lista, repetição,
  nº de lembretes, ▶ cronômetro); descrição com 2 linhas e "Ver tudo"; checklist compacto; dois botões
  grandes Concluir (com ▾ → "Concluí em outro dia…", calendário até hoje) e Adiar; fileira de 5 ícones:
  Não será feita, Editar, Lembretes, Lista, Excluir. Sem botão Fechar (fecha tocando fora ou no Voltar).
  Sai o campo "Data em que foi feita" sempre visível e os chips soltos de lista (Lista abre escolha).
  Tarefa resolvida: faixa "Feita hoje…" com Desfazer no lugar dos botões; Lembretes desabilitado.
- Histórico (registros e correções) sai dos detalhes da tarefa. Vai para uma aba de Histórico futura,
  com estatísticas e mais — feature separada, ainda sem spec. Os registros continuam sendo gravados.
- O Adiar entra nos detalhes da tarefa.
- O "Adiar" da notificação continua adiando só o lembrete.
- Folha do Adiar: só "Amanhã" e "Escolher data" (calendário a partir de hoje, sem o prazo atual);
  mantém o horário; tarefa repetida adia só esta ocorrência.
- Adiar tarefa vencida não pede confirmação: a folha avisa que o prazo antigo fica registrado como perdido,
  sem penalidade; "Desfazer" no aviso reverte.
- Prazo de semana/mês: sem Adiar por enquanto (só tarefas com dia específico).
- Adiar existe só no pop-up de detalhes; sem atalho no cartão (nem segurar o círculo, nem ⋮).
- Desfazer o adiamento de tarefa vencida: o registro de prazo perdido fica no histórico (só acrescenta).
- Lembretes ao adiar: relativos acompanham o prazo; de data fixa também são adiados (mesmo horário, data
  deslocada pelos mesmos dias); dias da semana não mudam.

## Pendências
- Nenhuma. Prompt pronto em `TODO/specify_adiar_detalhes_tarefa_app.md`.
