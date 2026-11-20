# Verificação · Backup portátil (spec 037)
Data: 2026-11-19. Implementação iniciada no Codex e concluída no Claude Code no mesmo checkout.

## Implementado
- `core/backup/`: `BackupArchive`/`BackupZip`/`BackupPasswordKey` (ZIP versionado com soma SHA-256 no comentário;
  envelope `TABITOS!` com AES-GCM, PBKDF2-SHA256 600 000 iterações, sal e nonce novos por exportação; fallback
  HMAC para API 24/25). Proteção detectada pelo conteúdo, não pelo nome.
- `BackupDocument` (formato 1: instante, linhas, preferências) e `BackupPreferences` (DataStore tipado, códigos fixos).
- `RoomBackupRows`: captura tipada das 32 tabelas do schema 7 em transação; cronômetro contando vira pausado com o
  trecho fechado no instante da cópia; prévia em savepoint desfeito; substituição com valores vinculados, validação
  de booleanos, tamanho de texto e vínculos (`BackupValidation`); conclusão pendente de cronômetro marcada como
  transferida; lembretes vencidos ficam cancelados para não gerar rajada.
- `RoomBackupRepository` + `BackupJournal`: SAF por URI, exportação grava com `"wt"`, prévia e restauração sob um
  mutex; substituição exige confirmação quando há dados pessoais (histórico, preços alterados, resumo ou
  preferências). Diário em `noBackupFilesDir` com impressão digital das linhas é gravado antes do commit; a abertura
  do app (`EthosApplication`) conclui as preferências só se o commit aconteceu. Depois da importação, lembretes e
  cronômetros são reconciliados.
- `features/backup/`: `BackupViewModel` (estados, senha só em memória, nome `Tabitos-backup-AAAA-MM-DD_HH-mm.zip`
  ou `.zip.enc`) e `BackupSettings` em Configurações → Dados (exportar, importar, senha, prévia, Substituir e
  importar com opção de exportar antes, progresso e avisos). Ícones `ic_upload`/`ic_download`.

## Testes
- JVM: `BackupArchiveTest` (4), `BackupDocumentTest` (4), `BackupViewModelTest` (3), `ExportProtectionTest` (2)
  aprovados; suíte JVM completa: 346 testes, 0 falhas.
- Instrumentados compilam (corrigido `use` em `RoomDatabase`), não executados (exigem autorização): `RoomBackupRowsTest` (cópia pausa só a cópia,
  prévia não altera destino, repetição idempotente, truncado rejeitado, preços iniciais, impressão digital e
  lembretes vencidos).

## Não executado / limites
- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` aprovados (lint: 0 erros, 40 avisos antigos, nenhum
  nos arquivos do backup). APK debug instalado no celular do usuário com `adb install -r` (dados preservados).
- Testes instrumentados não executados (aguardam autorização).
- Fluxo real Ethos → Tabitos (troca de `applicationId`) não testado em aparelho; FR-06 mantém o identificador atual.
- Interrupção entre commit e DataStore coberta por desenho, sem teste automatizado de morte de processo.
