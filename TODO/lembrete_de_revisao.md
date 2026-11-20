# Lembrete de revisão

Decisão de interface implementada na [spec 036](../specs/036-daily-review-reminder/spec.md).
O lembrete substitui o resumo diário da [spec de lembretes](../specs/007-reminders-notifications/spec.md).

## Configurações

**Lembrete de revisão**  
No horário escolhido, você recebe um aviso para marcar as tarefas e os hábitos que fez no dia.

A opção continua facultativa. Quem já escolheu um horário conserva essa escolha.

## Notificação

**Ficou algo sem marcar hoje?**  
Toque para abrir Hoje e marcar as tarefas e os hábitos que você fez.

- No horário escolhido, conferir se ainda há algo para marcar em **Hoje**. Se estiver tudo registrado
  ou o dia estiver vazio, pular o aviso daquele dia. Alterações posteriores não disparam um aviso atrasado.
- Usar o texto acima sem contagens. Tocar abre **Hoje** e encerra o aviso daquele dia.
- Manter o aviso no painel como notificação contínua. Se continuar lá, retirá-lo à meia-noite local.
  O próximo aviso só pode aparecer no horário escolhido do dia seguinte.
- No Android 14 ou superior, o sistema permite dispensar a notificação por gesto com o aparelho
  desbloqueado. Respeitar a dispensa pelo restante do dia.

Para aplicar “algo para marcar”, a referência é a tela **Hoje** com o filtro **Tudo**. Considerar
tarefas abertas exibidas ali e bons hábitos disponíveis para registro que ainda não foram marcados
na data. Uma tarefa apenas em **Revisar** ou a ausência de um registro de mau hábito não justificam
o aviso por si sós.
