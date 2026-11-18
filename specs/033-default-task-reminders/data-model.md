# Modelo de dados

**Preferência de novas tarefas.** Conjunto de antecedências em minutos para prazos com horário e
minuto local opcional para prazos com dia e sem horário. Ausência da preferência assume {30} e horário
opcional desligado. Gravar os campos em uma única atualização preserva as demais preferências.

**Lembrete de tarefa.** Cada opção aplicável gera uma definição RELATIVE com grupo próprio, ID da tarefa,
escopo da série desde a primeira ocorrência e instante da criação. Uma nova recorrência herda essas definições.
O sininho pode registrar uma exceção ou desativação por ocorrência ou a partir de uma ocorrência.

**Entrega.** O agendador existente identifica o aviso por grupo, ocorrência e dia. Um horário vencido na
criação não gera entrega retroativa. Tarefa concluída, remoção ou remarcação passam pela reconciliação existente.
