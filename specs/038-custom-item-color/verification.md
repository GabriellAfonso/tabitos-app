# Verificação da feature 038

Data: 2026-11-20.

## Executado

- Testes JVM filtrados (`core.colors.*`, `core.ui.components.*`, `core.habits.*`, `core.tasks.*`,
  `features.habits.*`, `features.tasks.*`): **161 aprovados, 0 falhas**, incluindo os novos `ItemColorTest`,
  `ItemColorRulesTest` e `CustomColorPresentationTest`. Compilação de `main` sem avisos nos arquivos novos.

- Verificação completa autorizada pelo usuário: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` com
  sucesso; **372 testes JVM aprovados, 0 falhas**. Lint: 42 avisos, todos anteriores (PluralsCandidate,
  versões de dependências, ícone do launcher, ModifierParameter em TrackerFormPage); nenhum nos arquivos da 038.
- Instalado com `installDebug` (atualização, dados preservados) no celular do usuário (2201117TG, Android 13), com
  autorização.

## Não executado

- Testes instrumentados, emulador e celular: roteiro manual em [quickstart.md](quickstart.md) pendente, incluindo
  roda por toque/arraste, linha em 360 dp, TalkBack e fonte 200%.

## Observações

- A linha de cores divide a largura em 6 células de 48 dp de altura para nunca quebrar; no diálogo de lista em
  telas de 360 dp cada célula fica com ~46 dp de largura (um pouco abaixo de 48 dp). Conferir no celular.
- Aviso de contraste removido por completo a pedido do usuário (2026-11-20); a prévia nos dois temas continua.
- Itens existentes mudam de tom (Sálvia → Verde etc.) sem regravar dados; Rosa/Cinza inalterados.
