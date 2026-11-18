# Marcação direta de tarefas e seção Concluídas

Autorizado pelo usuário em 2026-11-10, após implementação da spec 011.

## Decisões
- “Não será feito” também vai para Concluídas, com X no lugar do visto. Hoje e Revisão usam
  o dia local do registro de não realização; listas mostram os resultados da própria lista.
- Checkbox da tarefa conclui com data de hoje sem abrir detalhes, confirmação financeira
  ou confirmação de checklist pendente. Não marcar automaticamente os itens do checklist.
- A seção **Concluídas (n)** começa recolhida ao fim do conteúdo. Ao expandir, os cartões aparecem
  com texto riscado e cores discretas. Checkbox marcado desmarca e devolve às pendentes.
- Tocar no cabeçalho expande ou recolhe os cartões com animação.
  A seta gira entre direita (recolhida) e baixo (expandida).
  A contagem fica visível nos dois estados, e o TalkBack anuncia o estado da seção.
- Nas listas, respeitar a lista atual e a ordem manual ao retornar. Hoje mostra conclusões
  realizadas hoje; Revisão mantém acessíveis as conclusões feitas hoje de tarefas vencidas.
  Não carregar todo o histórico em Hoje/Revisão; ocultar tarefas com filtro Hábitos.
- Recorrências continuam por ocorrência; desfazer respeita dependências existentes, sem
  apagar ações feitas em ocorrências posteriores. Ganhos/estornos mantêm valores históricos.
- Esclarecimento do usuário: maus hábitos continuam com Registrar e popup de confirmação.
  Não adicionar desfazer no cartão nem alterar contagem/regras de ocorrências.
- Fluxos de detalhes, Não será feita e notificações mantêm confirmações existentes.
- Sem migração, dependências novas, reset de dados ou commits. Instalar atualização no celular.

## Aceite
Segurar a checkbox de uma tarefa pendente abre “Feito” e “Não será feito”. Feito usa a conclusão
rápida; Não será feito usa Resolve com a data de hoje e mantém as confirmações financeiras.
O toque simples e a ausência de realce na área de toque são preservados.
1. Marcar tarefa com checklist incompleto pelo cartão não abre modal e a move a Concluídas.
2. Desmarcar em Concluídas a devolve às pendentes; sem duplicação de ganho/estorno em retry.
3. Concluídas fica após os hábitos em Hoje e respeita data/lista/filtro.
4. Maus hábitos preservam o popup e as regras de múltiplas ocorrências.
5. Verificar regras/Room/UI e compilar, testar, lintar e instalar sem apagar dados.
