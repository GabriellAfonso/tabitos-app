# Plano de implementação · Backup portátil
Spec 037 em `spec.md`. Android API 24–36, Kotlin, módulo :app, Room 2.8 e DataStore existentes. Execução autorizada no checkout atual.

## Resumo e estrutura
Em `core/backup/`, exportar as linhas tipadas do schema conhecido com parâmetros vinculados, as preferências tipadas e o instante do relógio. O contêiner tem versão própria. Validar o arquivo antes da prévia. Normalizar na cópia o trecho aberto pelo relógio monotônico e checkpoint. Usar uma transação Room para substituir os registros. Coordenar as preferências com um diário de recuperação privado. Serializar importação com reconciliação de lembretes. Executar E/S fora da thread de interface.
Em `features/backup/`, o ViewModel mantém a operação e a prévia validada em memória. A UI usa o seletor de documentos. Conectar os controles em EthosApp na seção Dados.

## Constitution Check antes e depois do design
I. Requisitos e exclusões registrados antes da implementação.
II. Acesso offline, cópia lógica do banco e DataStore, recuperação ao reabrir. Schema de domínio preservado.
III. Repositório Hilt, ViewModel e conteúdo Compose. Relógios injetados.
IV. Arquivos pessoais sob controle do usuário, proteção opcional, textos em recursos, controles com rótulos.
V. Testes JVM dos formatos e falhas, instrumentados com dados fictícios para persistência. Execução Android e verificação completa dependem de autorização.

## Dependências
O formato e o inventário antecedem o repositório. A confirmação depende da prévia calculada no destino. A verificação reúne histórias e recuperações. Consulte research.md, data-model.md, contracts/backup.md e quickstart.md.
