# Quickstart — validar 039

## Testes JVM (sempre)

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.tabitos.app.core.update.*" --tests "com.tabitos.app.features.update.*"
```

O resultado esperado é que passem os testes de transições, recusa, falha, download pendente, checagem única por
processo e bloqueio do "Reiniciar" com cronômetro contando.

## Build debug

O app instalado por Android Studio ou adb não é da Play, então a checagem devolve `NotAvailable` e nada aparece. Isso
valida o FR-009, ou seja, a falha silenciosa.

## Fim a fim (só pela trilha de teste interno da Play)

1. Publique a versão N no teste interno e instale pela Play.
2. Publique N+1 (versionCode maior).
3. Abra o app. Depois do Hoje, aparece o pedido da Play. Aceite.
4. Use o app durante o download. Ao terminar, aparece "Atualização pronta · Reiniciar".
5. Com um cronômetro contando, a faixa diz "Disponível quando o cronômetro parar". Pause o cronômetro e o
   "Reiniciar" aparece.
6. Toque "Reiniciar". O app reabre na versão N+1 com a rotina intacta.
7. Repita recusando o pedido. Nada aparece até fechar o app por completo e abrir de novo.

Até a primeira publicação com a feature, os passos 1 a 7 ficam registrados como **não executados**.
