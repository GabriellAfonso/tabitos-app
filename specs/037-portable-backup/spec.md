# Backup portátil da rotina
**Feature** 037-portable-backup · **Data** 2026-11-19 · **Origem** `TODO/specify_backup_app.md`

## Cenários de usuário e aceitação

### US1 · Exportar a rotina (P1)
Em Configurações → Dados, a pessoa escolhe **Exportar backup**, decide se quer **Proteger com senha** e escolhe onde salvar. A cópia continua acessível fora do Ethos. Um arquivo sem senha pode ser lido por quem o obtiver. Perder a senha impede recuperar a cópia protegida. O sucesso só aparece depois de gravar o arquivo inteiro.

### US2 · Importar no Tabitos (P1)
A pessoa escolhe **Importar backup** e abre o arquivo, mesmo renomeado. O app identifica a proteção pelo conteúdo, solicita a senha quando necessária e exibe a data e as quantidades de listas, tarefas e hábitos. Em instalação nova, **Importar** conclui. Em destino com dados, o aviso oferece cancelar, exportar a rotina atual ou **Substituir e importar**. Dados iniciais automáticos permitem o fluxo de instalação nova. Histórico ou preferências pessoais já justificam confirmação.

### US3 · Continuar de onde parou (P1)
Ao exportar com um cronômetro contando há 12 minutos, o relógio de origem segue contando. Mesmo no dia seguinte a sessão importada volta pausada nos 12 minutos, pronta para retomar. Os próximos lembretes seguem as permissões e a agenda locais. Avisos antigos não formam uma fila de notificações imediatas.

## Requisitos funcionais

- FR-01. Oferecer backup manual offline, em arquivo único com nome sugerido `Tabitos-backup-AAAA-MM-DD_HH-mm.zip` conforme o horário local. Acrescentar `.enc` se protegido. O arquivo atravessa a mudança de identificador.
- FR-02. Preservar listas e ordem, tarefas, séries, checklists, prazos, hábitos, regras e períodos, registros, revisões, acompanhamentos versionados e respostas, históricos, compras, recompensas, moedas, XP, sessões e trechos, configurações de lembretes e preferências. Manter datas e vínculos.
- FR-03. A cópia representa um instante consistente. Repetir a importação produz o mesmo resultado sem lançar compras, recompensas ou conclusões.
- FR-04. Validar o conteúdo antes de substituir. Senha incorreta, arquivo danificado, incompleto ou incompatível, cancelamento ou falha de armazenamento preservam a rotina anterior. Uma interrupção deixa uma rotina completa ao reabrir.
- FR-05. Expor operações, progresso e erros em português com controles acessíveis. Permissões do Android pertencem à instalação.
- FR-06. A primeira entrega mantém o identificador e nome atuais. A troca de nome, a mesclagem de rotinas e a sincronização ficam fora do escopo.

## Entidades e pressupostos
A cópia contém a rotina e suas preferências. A prévia identifica o arquivo antes da confirmação. A instalação de origem pode coexistir com Tabitos. Testar com dados fictícios.

## Critérios de sucesso
1. Os dois formatos recuperam todas as categorias e seus vínculos sob outro identificador, mesmo com o arquivo renomeado.
2. Importar duas vezes preserva saldo, totais, histórico e preferências.
3. Cancelar a substituição ou selecionar arquivo inválido mantém os dados adicionados após o backup.
4. O exemplo de 12 minutos recupera o tempo acumulado sem contar o intervalo entre instalações.
