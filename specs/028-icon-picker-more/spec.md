# Spec 028 — Seletor de ícones com "Ver mais"

Estado: implementado (2026-11-17). Só interface e catálogo de ícones; sem mudança de schema (7).
Protótipo: `prototypes/seletor-icones/index.html` (opção A escolhida).

## Motivo

Os seletores de ícone mostravam todas as opções de uma vez: 6 nas listas, 4 nas recompensas e 17 nos hábitos,
que já ocupavam quatro linhas do editor. O usuário pediu mais variedade sem aumentar esse espaço.

## Decisões

- **Uma linha + "Ver mais"** nos três editores (lista, hábito, recompensa): até 5 ícones principais e um botão
  circular `more_horiz` com o mesmo formato, descrito como "Ver mais ícones".
- **Principais fixos por editor/tipo** (não "usados por último", que exigiria guardar histórico):
  - Lista: Lista, Trabalho, Casa, Estudo.
  - Recompensa: Presente, Jogos, Refeição, Café.
  - Bom hábito: Folha, Água, Academia, Estudo, Meditação.
  - Mau hábito: Proibido, Cigarro, Bebida, Tempo de tela, Fast food.
- Se o ícone atual não é principal, ele ocupa o primeiro lugar da linha (selecionado) e o último principal sai,
  para o ícone atual estar sempre visível sem aumentar a linha.
- **Pop-up em folha inferior** (`ModalBottomSheet`), título "Ícones", busca por nome em português (sem
  diferenciar acentos/maiúsculas) e chips de categoria que rolam até a seção. Tocar num ícone escolhe e fecha;
  fechar/voltar não muda nada. O ícone atual aparece marcado. Busca e categoria não são salvas.
- **Catálogo único** (`core/icons/AppIcon`, ~127 ícones) em 11 categorias: Geral, Casa, Trabalho e estudo,
  Saúde, Esporte, Comida, Dinheiro e compras, Lazer, Pessoas, Viagem, Evitar. Cada ícone tem um nome único,
  lido pelo TalkBack e usado na busca. Alguns nomes antigos foram unificados (ex.: `star` era "Pessoal" nas
  listas e "Estrela" nos hábitos; agora é "Estrela").
- Ícones Material Symbols Rounded peso 400 como vector drawables (`res/drawable/ic_*.xml`), sem dependência
  nova, como na spec 019. Os 102 desenhos novos vieram do SVG oficial de 24 px em
  `fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/<nome>/default/24px.svg` (Apache 2.0).

## Verificação (2026-11-17)

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` aprovados; 254 testes JVM; sem avisos de lint novos.
- Testes instrumentados e uso no aparelho não executados.

## Preservação de dados

- `TaskList.icon` e `Habit.icon` continuam sendo índices. As tabelas `AppIconTables.list`/`habit` começam
  com as opções antigas na mesma ordem e seguem com os demais ícones na ordem de declaração do `AppIcon`.
  Ícones novos só podem ser acrescentados no fim do enum; um teste fixa as tabelas.
- `Reward.icon` continua sendo chave de texto: é o nome do `AppIcon`. As chaves antigas `GIFT`, `GAME`, `FOOD`
  e `REST` são entradas do enum com o mesmo desenho de antes. Entradas do enum nunca são renomeadas.
- Validações de domínio aceitam o tamanho das novas tabelas e qualquer chave do catálogo.

## Fora do escopo

- Ícone próprio para tarefas (exigiria coluna e migração).
- Ícones "usados por último", ícones personalizados ou cores por ícone.
- Ícones das notificações e do launcher.

## Critérios de aceite

- Os três editores mostram uma linha de principais + "Ver mais"; nenhum ícone salvo antes muda de desenho.
- Escolher no pop-up um ícone fora dos principais salva e aparece no primeiro lugar da linha.
- Busca "agua" encontra "Água"; busca sem resultado mostra aviso.
- Mau hábito novo começa com Proibido em vermelho.
