# Verificação — 039 Aviso de atualização

**Data:** 2026-11-20

## Executado

- Testes JVM: `.\gradlew.bat :app:testDebugUnitTest --tests "com.tabitos.app.core.update.*" --tests
  "com.tabitos.app.features.update.*"`. Resultado: 10 testes aprovados (8 + 2), 0 falhas. O build compilou o código principal e o grafo Hilt.
  - `AppUpdateControllerTest` cobre:
    - sem atualização;
    - aceitar → baixando → pronta → instalar;
    - pedido iniciado uma só vez;
    - recusa absorvente, mesmo se o download terminar;
    - pedido que não inicia conta como recusa;
    - download pendente de abertura anterior → pronta, e "Agora não" impede instalar;
    - falha do download → nada;
    - loja consultada uma vez por processo.
  - `AppUpdatePresentationTest` cobre: "Reiniciar" só sem cronômetro contando; as outras fases não mostram faixa.

## Não executado

- Fluxo fim a fim pela trilha de teste interno da Play (quickstart.md, passos 1–7). Só é possível depois de publicar
  uma versão com a feature e outra maior.
- Build debug, lint e a suíte JVM completa. A verificação completa precisa de permissão do usuário.
- Testes instrumentados e TalkBack em aparelho.
- Medição do "Fully drawn" (SC-001) no celular.

## Limitações

- Em instalações fora da Play (debug, adb), a checagem devolve "sem atualização" e nada aparece. Isso é esperado
  pelo FR-009.
- A faixa espera a primeira leitura dos cronômetros antes de aparecer, então "Reiniciar" nunca pisca com um
  cronômetro contando.
