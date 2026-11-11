# Hábitos diretos e dados de demonstração

Status: autorizado para implementação pelo usuário em 2026-11-10.

## Escopo e regras
- Hábitos de prática: marcar e desmarcar o dia atual sem confirmação financeira.
  Preservar transações, estornos e valores históricos das features 004/005.
- Hoje mostra somente estado e ocorrências do dia: sem barras semanais, progresso do
  período ou agrupamento por meta cumprida. Permitir desmarcar no mesmo cartão.
- Calendário, progresso do período e histórico ficam nos detalhes do hábito.
- Evitar/limitar mantêm ocorrência como ação distinta e confirmação financeira.
- Configurações debug oferecem restauração de dados fictícios com confirmação que
  informa substituição de TODO o conteúdo/progresso local; preservar tema/permissões.
- Restauração explícita, transacional e repetível; datas relativas ao dia do reset.
  Falha preserva a base anterior. Nenhuma carga automática na abertura/atualização.
- Incluir listas, tarefas, checklist, prazos, recorrência, hábitos dos três tipos,
  históricos e revisão, carteira/XP por ações reais, recompensas/compra, metas de
  tempo e acompanhamentos com respostas históricas. Sem cronômetro/avisos ativos.
- Bloquear restauração com cronômetro contando; cancelar avisos antigos após sucesso.
- Instalar atualização debug no celular preservando dados; carga pelo botão explícito.

## Aceite
1. Marcar/desmarcar atualiza registros e carteira sem modal ou ganhos duplicados.
2. Hoje não mostra outras datas nem oculta hábito após meta cumprida.
3. Detalhes têm calendário/progresso; evitar/limitar mostram contagem de hoje.
4. Cancelar reset não escreve; repetir restaura sem duplicar; falha faz rollback.
5. Executar build, JVM, lint e integração de reset/estornos disponíveis.

Esta decisão substitui a confirmação obrigatória da prática rápida na spec 010.
Revisão, evitar/limitar, tarefas e compras mantêm suas confirmações.
