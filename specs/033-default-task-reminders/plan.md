# Plano de implementação da feature 033

Branch de trabalho `master` · 2026-11-18 · [Especificação](spec.md)

## Abordagem

Ler a preferência do DataStore ao criar uma tarefa com prazo de dia. Gravar os lembretes relativos
e a tarefa na mesma transação Room, sob a proteção de identidade que o repositório já usa para criação.
Uma nova série recebe uma regra por antecedência, aplicável às ocorrências a partir da primeira.
Para um lembrete cujo instante passou, a data de criação limita as entregas, inclusive na recuperação.

## Contexto técnico e organização

Android, Kotlin, módulo `:app`, Compose Material 3 e Hilt. A preferência reside em `core/preferences`.
A geração de definições pertence a `core/reminders/domain` e a gravação a `core/tasks/data/RoomTaskRepository.kt`.
O usuário escolhe os padrões em `features/reminders` e ajusta exceções no sininho existente.
A criação usa o fluxo de permissão de notificações em contexto. DataStore e Room já existem no projeto.
Nenhuma migração é necessária. A instalação de uma atualização não percorre as tarefas existentes.

## Constitution Check

Antes e após o desenho, os requisitos FR-001 a FR-007 têm cobertura no domínio, persistência ou interface.
O domínio recebe entradas explícitas. O componente de configurações recebe estado e callbacks.
A interface usa recursos em português e controles acessíveis. A preferência e os lembretes funcionam offline.
O teste de persistência usa dados fictícios. A rotina de verificação inclui testes JVM e registra os limites
dos testes que dependem de Android. A implementação foi solicitada expressamente.

## Entrega

Validar primeiro a criação com o padrão inicial, depois a persistência da preferência e os ajustes pelo sininho.
Consultar [decisões](research.md), [modelo](data-model.md), [contrato de interface](contracts/interface.md)
e [roteiro de verificação](quickstart.md).
