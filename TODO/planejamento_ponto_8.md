# Ponto 8 — cronômetros e metas de tempo

Design consolidado. Prompt salvo em `TODO/specify_cronometros_metas_tempo_app.md` em 2026-11-07.
Salvar o prompt não executa Spec Kit nem autoriza implementação.

## Decisões aprovadas

- Permitir cronômetro crescente e contagem regressiva, com iniciar, pausar e retomar.
- Apenas um cronômetro ativo contando por vez. Para iniciar outro, pausar o atual.
- Continuar contando com a tela bloqueada e app em segundo plano. Mostrar notificação
  de sessão com pausa e acesso ao app.
- Ao atingir o tempo, avisar e pedir confirmação para concluir tarefa/meta; não conceder
  moedas automaticamente apenas porque o contador chegou ao objetivo.
- Permitir adicionar tempo esquecido e corrigir sessões manualmente, com histórico de alterações.
- Recompensa temporal: confirmar compra desconta moedas e inicia a contagem imediatamente,
  conforme uso imediato já aprovado. Ao acabar, apenas avisar, sem bloquear outros aplicativos.
- Estes usos de tempo já constam de `TODO/planejamento_lembretes_tempo.md`: meia hora de leitura,
  três horas semanais de atividade e recompensas como duas horas de gameplay.

## Detalhes finais aprovados

- Somar várias sessões para atingir a meta, excluindo pausas. Ex.: quarenta, cinquenta e noventa
  minutos completam três horas semanais.
- Distribuir tempo pela data em que foi realizado, dividindo sessões na virada do dia/período.
  Ex.: domingo 23h50 até segunda 00h20 atribui dez minutos à semana anterior e vinte à nova.
- Ao atingir o tempo objetivo, pausar a contagem e pedir confirmação para concluir tarefa/meta.
  Não conceder moedas/XP automaticamente nem contar tempo adicional sem ação do usuário.
- Após reinício do aparelho, recuperar tempo salvo e deixar sessão pausada, sem contar o período
  em que o aparelho esteve desligado. Isso não muda a continuidade com tela bloqueada/segundo plano.
- Permitir pausar e retomar tempo de recompensa já comprado, sem nova cobrança nem devolução.
  A sessão é iniciada na compra; retomar essa sessão não é resgatar uma nova recompensa.
- Se outro cronômetro estiver ativo ao comprar recompensa temporal, pedir confirmação de sua pausa
  antes de cobrar e iniciar a recompensa. Cancelar essa transição não pode cobrar a compra.

## Estado

Seis propostas iniciais e quatro detalhes finais aprovados. Prompt consolidado;
não reabrir essas decisões como perguntas de produto durante a execução.
