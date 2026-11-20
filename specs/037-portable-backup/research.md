# Decisões técnicas

- O seletor de documentos disponibiliza apenas a URI escolhida e permite salvar fora da instalação. O identificador do pacote fica fora do protocolo. [Android SAF](https://developer.android.com/training/data-storage/shared/documents-files)
- ZIP versionado e, quando solicitado, envelope autenticado por senha com sal e nonce novos por exportação. AES-GCM e PBKDF2, compatíveis com o SDK mínimo mediante escolha de provedor compatível, dispensam uma chave vinculada ao aparelho. Validar o diretório final do ZIP e limitar entrada/descompressão. [Criptografia Android](https://developer.android.com/privacy-and-security/cryptography)
- O schema 7 contém dados de domínio em tabelas com colunas TEXT/INTEGER. Importar apenas tabelas e colunas do contrato local por parâmetros. A cópia de um arquivo .db aberto poderia perder páginas do WAL. Preservar ordem e IDs.
- O relógio de sessão já calcula o término de trecho com informações de boot e checkpoint. Aplicar essa regra à representação exportada. As tabelas contábeis podem conter linhas de inicialização automática que precisam ser reconhecidas para a prévia.
- A transação SQLite cobre as linhas. Um diário persistente de preferências permite compensar uma escrita DataStore interrompida antes do commit. A abertura do app conclui a recuperação antes dos observadores.
