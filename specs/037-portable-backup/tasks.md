# Tarefas · Backup portátil
Caminhos de código relativos a `app/src/main/java/com/ethos/app/`.

## Fundação
- [x] T001 Fixar contrato do repositório, prévia e confirmação em core/backup/BackupRepository.kt.

## US1 · Exportar
- [x] T002 [P] [US1] Escrever casos de formatos, senha e corrupção em app/src/test/java/com/ethos/app/core/backup/.
- [x] T003 [US1] Implementar contêiner portátil e criptografia em core/backup/BackupArchive.kt.
- [x] T004 [P] [US1] Cobrir cópia e transações com base fictícia em app/src/androidTest/java/com/ethos/app/core/backup/.
- [x] T005 [US1] Implementar inventário, captura tipada e contagens em core/backup/RoomBackupRows.kt.
- [x] T006 [US1] Capturar preferências e montar documento consistente em core/backup/.

## US2 · Importar
- [x] T007 [US2] Validar antes da prévia e recuperar substituição interrompida em core/backup/ e EthosApplication.kt.
- [x] T008 [P] [US2] Implementar estados, confirmação, seletor, senha e recursos em features/backup/, EthosApp.kt e res/values/.

## US3 · Retomar rotina
- [x] T009 [US3] Normalizar cronômetro durante a captura em core/backup/RoomBackupRows.kt.
- [x] T010 [US3] Reconciliar lembretes após importação em core/reminders/platform/ e core/backup/.

## Integração
- [x] T011 Verificar testes JVM e registrar evidências e limites em specs/037-portable-backup/verification.md.

Após T001, formato, linhas e interface podem ser desenvolvidos em paralelo pelos integrantes. T006/T007 integram os contratos e T011 depende das três histórias. MVP para migração inclui as três histórias.
