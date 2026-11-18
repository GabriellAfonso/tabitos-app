# Editores de criação (tarefa, bom hábito, mau hábito)

Protótipo: `prototypes/editores-criacao/index.html`
(publicado em https://claude.ai/artifact/RXfpqyfZCsqwLdrukcfHHx).

## Decidido

- 2026-11-17, rodada 1: a direção escolhida é **A · Folha rápida**. O editor abre como uma folha inferior por cima
  de Hoje, com foco no nome e o teclado aberto. Os chips de atalho mostram o valor atual e cada um abre uma folha de
  escolha. As opções menos usadas ficam em "Mais". As propostas B (linhas agrupadas) e C (frase guiada) foram
  descartadas como direção principal.
- Continuam valendo as regras atuais: mapeamento de repetição e frequência, nada de checklist em itens com
  cronômetro, datas e horas só por seletor, Dificuldade e estimativa escondidas com o modo jogo desligado, mau hábito
  que começa em Vermelho com o ícone de proibido, escolha de escopo nas recorrentes, confirmação de revisão e nenhuma
  mudança de schema.

- 2026-11-17, rodada 2: aprovado **A2 · Folha que cresce**. Criar abre a folha compacta; "Mais" e editar abrem
  em tela cheia, com linhas rótulo → valor. O lembrete fica fora do editor (continua no pop-up de detalhes) e o
  tipo é escolhido só pelo "+".
- Implementado na spec 029 (`specs/029-editor-growing-sheet/`), com build, testes JVM e lint aprovados.

## Pendente

- Rodar os testes instrumentados e conferir no celular: teclado ao abrir, fonte a 200%, TalkBack e tema escuro.

Protótipos não autorizam implementação nem tornam regras aprovadas.
