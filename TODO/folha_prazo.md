# Folha de prazo do editor de tarefa

Estado: decidido — opção C aplicada no app em 2026-11-19 (contrato em
`specs/035-deadline-until-date/contracts/interface.md`). Pendente só validação no celular. Protótipo: `prototypes/prazo/index.html`
(publicado em https://claude.ai/artifact/78wBtWcW6i3iEYMGedZrBG).

## Contexto

- O usuário não gostou da folha Prazo atual (lista de cinco opções + campos Data/Começa em/Horário).
- Correção já aplicada no app (spec 035): "Começa em" ficava preso em hoje porque o seletor limitava o início à data
  final, que começa como hoje. Agora o início vai de hoje em diante e o fim acompanha se ficar antes.
- O usuário quer poder mudar o início, mas acha desnecessário mostrá-lo como campo fixo.

## Opções no protótipo

- A · Calendário na folha: atalhos + calendário embutido; chave "Mostrar todos os dias até lá"; "Começa hoje · mudar".
- B · Num dia ou até um dia (recomendada): abas Sem data / Num dia / Até um dia; Domingo/Sexta como atalhos de
  "Até um dia" (gravando os tipos semanais atuais); Outro dia com período no calendário.
- C · Lista com valores: lista atual com valores à direita, período em janela, início como texto "mudar".

## Pendências

- Escolha da opção (ou mistura).
- Calendário embutido na folha × janela.
- Atalho "Sábado" em B é palpite.
- Nada disso muda regras ou schema; vira spec própria depois da escolha.
