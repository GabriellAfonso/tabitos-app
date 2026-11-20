# Verificação

Executar a suíte JVM filtrada de core.reminders. Para validação manual autorizada em uma instalação de teste:

1. Conferir o nome e a descrição com o lembrete desligado, selecionar um horário e reabrir Configurações.
2. Com tarefa aberta em Hoje, observar uma única notificação contínua. Tocar a partir de Calendário e conferir Hoje.
3. Repetir com um bom hábito marcado, somente um mau hábito e um dia vazio. Adicionar uma tarefa após o horário.
   Todos esses casos devem ficar silenciosos.
4. Deixar uma notificação no painel até meia-noite. Conferir expiração e que a próxima aparece apenas no novo horário.
5. Dispensar por gesto no Android 14+, reabrir o app e conferir ausência de reentrega. Conferir bloqueio do canal,
   mudança de fuso, alarme aproximado e descrição acessível com fonte ampliada.
