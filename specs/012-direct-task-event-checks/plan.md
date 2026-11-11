# Plano e Constitution Check

Reutilizar TaskRules, HabitRules, repositórios Room e ledger. Ação com estado desejado
para checkbox de tarefa permite retry idempotente. ViewModels serializam escrita e publicam
snapshot confirmado antes do próximo toque. UI somente apresenta estado/callbacks.
Separar seleção de conclusões por página em função testável; manter ordem de pendentes.
Checkbox de tarefa dispensa preview; hábitos/detalhes/revisão preservam seus fluxos.
Testes de recorrência, estorno, retry e navegação/semântica do checkbox.

Conforme constituição: escopo antes do código; Room offline/schema preservado; sem dependências;
textos em recursos, descrição de checkbox, cores discretas com legibilidade; teste por risco.
