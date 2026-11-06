<!--
Sync Impact Report
Version: template sem versão → 1.0.0 (adoção inicial).
Princípios: placeholders substituídos por I–V.
Seções adicionadas: Restrições técnicas; Fluxo de desenvolvimento; Governance.
Removidos: exemplos genéricos do template; nenhuma regra previamente adotada.
✅ .specify/templates/plan-template.md: gates constitucionais.
✅ .specify/templates/spec-template.md: escopo, dados e acessibilidade.
✅ .specify/templates/tasks-template.md: verificações por risco e limites de execução.
✅ AGENTS.md: contexto gerenciado atualizado.
✅ PLANO_BASE.md: adoção registrada.
.specify/templates/commands/ não existe; a integração usa skills locais.
Hooks before_constitution/after_constitution: não registrados.
Pendências de preenchimento: nenhuma.
-->
# Ethos Constitution

## Core Principles

### I. Especificação antes da implementação

- Toda nova funcionalidade DEVE ter escopo, regras e critérios de aceite em `specs/`
  antes da implementação. Mudanças de comportamento DEVEM atualizar a spec correspondente.
- O fluxo DEVE usar Spec Kit: constitution, specify, clarify quando necessário, plan,
  tasks, analyze e implement. Executar cada etapa conforme solicitação do usuário.
- Criar spec, plano ou tarefas NÃO autoriza implementar. Hooks não ampliam autorização.
- Documentos DEVEM distinguir decisões aceitas, propostas e trabalho implementado.
  `PLANO_BASE.md` é contexto inicial e NÃO substitui uma spec.

Razão: manter o desenvolvimento no escopo combinado e as decisões revisáveis.

### II. Dados locais confiáveis

- Operações centrais de tarefas, hábitos e rotinas DEVEM funcionar sem internet.
- Room DEVE ser a fonte de verdade dos dados estruturados; DataStore guarda preferências.
- Schemas Room DEVEM ser exportados e versionados. Evoluções de schemas existentes
  DEVEM definir migração e verificar preservação de dados; fallback destrutivo é proibido.
- Falhas de persistência DEVEM permitir recuperação e NÃO podem ser exibidas como sucesso.
- Recorrências e lembretes exigem regras especificadas de datas, horários, fuso,
  mudança de dia e comportamento após reinício quando aplicável.
- Backend, autenticação e sincronização só entram com necessidade e escopo especificados.

Razão: a organização pessoal precisa continuar disponível sem perder dados.

### III. Arquitetura simples e responsabilidades claras

- O app DEVE usar Kotlin, Compose, Material 3, uma Activity e Navigation Compose.
- Organizar funcionalidades em `features/` e infraestrutura compartilhada em `core/`.
  Features NÃO podem importar implementações de outras features.
- UI envia ações ao ViewModel; ViewModel acessa contratos de repositório. Use cases
  só DEVEM existir para encapsular regras de negócio relevantes.
- `domain/` NÃO pode depender de Android, Compose, Room ou ViewModel.
- Hilt e KSP DEVEM fornecer injeção e geração de código. Usar Coroutines/Flow,
  StateFlow imutável e `viewModelScope`; NÃO usar `GlobalScope`.
- Novas camadas, dependências e módulos DEVEM resolver necessidade documentada no plano.

Razão: separar regras, persistência e interface sem abstrações sem uso concreto.

### IV. Interface acessível e privacidade

- Separar a Screen que coleta estado do conteúdo Compose que recebe dados e callbacks.
  Coletar com `collectAsStateWithLifecycle()`; NÃO acessar banco em Composables.
- Representar carregamento, vazio, conteúdo e erro quando aplicáveis.
- Oferecer tema claro, escuro e sistema. Textos visíveis DEVEM estar em português
  em recursos; código e identificadores DEVEM estar em inglês.
- Fluxos novos DEVEM considerar leitor de tela, descrição de ícones acionáveis,
  alvos de toque e fontes ampliadas; registrar os cenários verificados.
- NÃO registrar tarefas ou dados pessoais em logs nem versionar segredos ou keystores.
  `BuildConfig` NÃO protege segredos de um APK.
- Solicitar permissões somente quando exigidas por uma funcionalidade especificada.

Razão: a organização pessoal deve ser utilizável e preservar conteúdo privado.

### V. Verificação proporcional e evidência honesta

- Regras de negócio novas ou alteradas DEVEM ter testes de casos e falhas relevantes.
- Persistência e migrações DEVEM ter verificação de integração quando afetadas.
- Alterações de interface DEVEM validar os critérios de aceite e acessibilidade aplicáveis.
- NÃO criar testes que apenas espelhem implementação. Mudanças triviais de documentação
  ou configuração exigem verificação adequada, sem testes artificiais.
- Depois de existir o app, executar build, testes e lint pertinentes às alterações.
  Relatar checagens não executadas e limitações concretas; NÃO declarar aprovação sem execução.
- Preferir fakes para regras isoladas. Testes de regressão DEVEM reproduzir a falha corrigida.

Razão: verificar riscos e comportamento reais com evidência compreensível.

## Restrições técnicas

- Usar Gradle Kotlin DSL, catálogo de versões e Wrapper para reproduzir o build.
- AGP 9 DEVE usar Kotlin integrado. Não copiar opções antigas sem necessidade demonstrada.
- Validar SDKs e compatibilidade de bibliotecas no plano técnico; NÃO usar versões dinâmicas.
- Separar debug/release e habilitar R8 na versão release.
- WorkManager serve para trabalho adiável; lembretes precisos exigem decisão própria.
- Consolidar pacote, SDK mínimo e organização de módulos no plano da fundação.
- Seguir Kotlin official e limite de 120 caracteres por linha.
- Da referência IPB, aproveitar convenções; NÃO copiar marca, credenciais ou regras da igreja.

## Fluxo de desenvolvimento e qualidade

1. Ler constituição, `AGENTS.md` e artefatos pertinentes da feature.
2. Especificar comportamento e critérios verificáveis sem inventar requisitos futuros.
3. Criar plano com Constitution Check antes da pesquisa e após o design.
4. Gerar tarefas rastreáveis, incluindo verificações exigidas pelo risco.
5. Analisar consistência e resolver contradições constitucionais antes da implementação.
6. Implementar quando solicitado, verificar critérios e relatar resultados e limitações.

Commits, push, tags, publicação e aumento de versão exigem solicitação explícita.
Hooks NÃO autorizam essas ações ou implementação. Usar Conventional Commits quando
commits forem solicitados. Não limpar caches como procedimento rotineiro.

## Governance

Esta constituição governa decisões técnicas e o fluxo do projeto. `AGENTS.md` contém
orientações operacionais compatíveis. Instruções explícitas do usuário prevalecem;
conflitos DEVEM ser expostos e a documentação alinhada sem ampliar o escopo solicitado.

Emendas DEVEM registrar motivo, impacto, versão, data e sincronização de templates e
artefatos afetados. Remoções/redefinições incompatíveis aumentam MAJOR; novos princípios
ou ampliação material aumentam MINOR; esclarecimentos sem nova regra aumentam PATCH.
A versão 1.0.0 é a adoção inicial, sem constituição anterior em vigor.

Planos e revisões DEVEM verificar conformidade com os cinco princípios. Registrar
desvios no Constitution Check; justificativa isolada NÃO revoga uma regra obrigatória.
Alterar uma regra exige emenda explícita ou instrução do usuário.

**Version**: 1.0.0 | **Ratified**: 2026-11-06 | **Last Amended**: 2026-11-06
