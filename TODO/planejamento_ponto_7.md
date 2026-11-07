# Ponto 7 — lembretes e notificações

Design consolidado. Prompt salvo em `TODO/specify_lembretes_notificacoes_app.md` em 2026-11-07.
Salvar o prompt não executa Spec Kit nem autoriza implementação.

## Decisões aprovadas

- Lembretes opcionais em tarefas e hábitos. Para tarefas: horário exato ou antecedência.
  Para hábitos: horários e dias escolhidos.
- Permitir vários lembretes por item, como um dia antes e trinta minutos antes.
- Adiar por dez minutos, trinta minutos, uma hora ou horário escolhido, sem mudar prazo
  nem gerar penalidade.
- Notificações de tarefa oferecem Concluir, Adiar e Abrir. Hábitos oferecem Abrir e Adiar,
  sem registrar prática ou ocorrência negativa diretamente por um toque.
- Incluir resumo diário configurável com tarefas, hábitos e pendências de revisão.
- Após aparelho desligado/permissão desativada, não disparar todos os avisos antigos ao voltar.
  Apresentar um resumo dos lembretes ainda relevantes, sem inferir descumprimento.
- Avisos de cronômetros pertencem ao ponto 8.

## Detalhes finais aprovados

- Resumo diário começa desativado. Ao ativar, o usuário escolhe horário. Mostrar itens de Hoje
  e quantidade de pendências de revisão; tocar abre o app.
- Cancelar avisos de tarefas concluídas, excluídas ou marcadas Não será feita e de hábitos
  pausados/excluídos. Metas de prática cumpridas não geram mais avisos naquele período;
  evitar/limitar continuam conforme a configuração de lembretes.
- Tarefas semanais sem dia definido usam dias/horários de lembrete escolhidos pelo usuário,
  sem modificar seu prazo. A agenda de avisos não cria um dia obrigatório de conclusão.
- Concluir pela notificação, havendo checklist pendente, abre o app para a confirmação já
  aprovada. A ação deve respeitar estado atual e efeitos reais da carteira; não contornar
  confirmações e regras anteriores nem duplicar conclusão/recompensa.

## Orientações para o plano técnico

- Permissões, agendamento Android e recuperação de alarmes são decisões do plano técnico,
  sem prometer entrega exata que o sistema não permita e sem serviços de nuvem não solicitados.

## Estado

Seis propostas iniciais e três detalhes finais aprovados. Prompt consolidado;
não reabrir essas decisões como perguntas de produto durante sua execução.
