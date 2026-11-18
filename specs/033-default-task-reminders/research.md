# Decisões de projeto

- O repositório de tarefas distingue Create de Refresh, Materialize e Edit. Reconstruir avisos ao observar
  tarefas também atingiria séries antigas e faria um aviso removido reaparecer. A fronteira de criação
  permite conservar o padrão escolhido naquele instante.
- O DataStore de preferências já é compartilhado por tema e modo jogo. Um conjunto vazio precisa representar
  a escolha de desligar os avisos, enquanto a ausência de chave representa a primeira utilização.
- Lembretes relativos já usam o prazo civil, grupo, escopo e instante de criação. Uma definição com
  antecedência zero e referência explícita cobre o horário optativo de uma tarefa só com data.
  O mesmo mecanismo permite remarcar, silenciar e editar avisos pelo sininho.
- O editor distingue prazo em um dia e prazo por período (semana/semana útil). Aplicar os padrões somente
  a um dia específico evita escolher implicitamente um dia da semana para notificar.
