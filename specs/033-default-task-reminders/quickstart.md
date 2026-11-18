# Verificação

1. Em dados de teste, comparar uma tarefa antiga com horário, uma com aviso próprio e uma série antiga
   antes e depois de mudar as preferências e gerar a próxima ocorrência.
2. Com o padrão inicial, criar uma tarefa para amanhã às 15h, uma só com data e uma sem prazo. Conferir
   no sininho respectivamente um aviso 30 minutos antes, zero e zero.
3. Selecionar também 1 dia e habilitar 9h para tarefas só com data. Criar tarefas para daqui a três dias.
   Conferir dois avisos com horário e um às 9h sem horário. Reabrir Configurações e conferir as escolhas.
4. Silenciar uma ocorrência e verificar a seguinte. Silenciar a série a partir da seguinte, reiniciar e
   conferir novamente. Remarcar e concluir uma tarefa e verificar seus avisos.
5. Com fonte ampliada e TalkBack, identificar opções, estados e ações. Verificar o aviso de bloqueio
   e o pedido contextual de permissão em instalação de teste.

Para JVM, usar `.\gradlew.bat :app:testDebugUnitTest`. Executar verificações em Android somente com autorização.
