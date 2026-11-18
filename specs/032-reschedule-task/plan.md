# Plano 032 · Remarcar tarefa

[Especificação](spec.md) · 2026-11-18 · `master`

## Summary

Estender o fluxo local de alteração de prazo. Derivar as escolhas rápidas do prazo e do dia fornecido pelo
ViewModel. Classificar a direção dentro da alteração transacional, antes de projetar o histórico.

## Technical Context

Android, Kotlin, Compose Material 3, min SDK 24 e target/compile SDK 36. Módulo existente `:app`, Room schema 7,
relógio injetado e os testes JUnit do projeto. A janela de desfazer e o seletor de datas já existem.

## Constitution Check

Antes do design, a especificação delimita a US1–US3 e os testes. Após o design, I–V atendidos pela sequência
spec/plano/tarefas, operação local atômica, separação regras/apresentação/Compose, textos pt-BR e testes específicos.
Validação visual e falada depende de execução Android autorizada.

## Project Structure

Em `app/src/main/java/com/ethos/app/`:

- `core/tasks/domain` classifica mudanças e correções. `core/tasks/data` mantém a mesma transação com lembretes.
- `core/reminders/domain` calcula datas civis e exclui da agenda o momento que já passou ao antecipar.
- `features/tasks` monta o modelo da folha e oferece o feedback após salvar. `features/statistics` projeta eventos.
- `app/src/main/res/values/strings.xml` reúne a cópia. As árvores test/androidTest espelham esses pacotes.

Detalhes em [pesquisa](research.md), [modelo](data-model.md), [contrato](contracts/task-detail.md) e
[roteiro de validação](quickstart.md).

## Complexity Tracking

Reutilizar infraestrutura instalada, sem novas dependências, tabelas ou permissões.
