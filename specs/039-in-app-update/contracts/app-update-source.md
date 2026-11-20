# Contrato interno — AppUpdateSource

A interface fica em `core/update/data`, porque recebe um launcher do androidx. A implementação que usa a Play fica
em `core/update/platform`. `StoreUpdate`, `AppUpdatePhase` e `AppUpdateRules` ficam em `core/update/domain`, que
continua puro.

```kotlin
interface AppUpdateSource {
    /** One query to the store; never throws. Failures and non-Play installs are NotAvailable. */
    suspend fun check(): StoreUpdate
    /** Download progress of a flexible update; the listener is removed when collection stops. */
    fun installStates(): Flow<StoreUpdate>
    /** Starts the store's consent flow for the update found by the last [check]; false if it could not start. */
    fun startFlexible(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
    /** Installs a downloaded update; the store restarts the app. */
    fun complete()
}
```

Garantias:

- `check()` e `installStates()` nunca propagam exceções da loja. Cancelamento de corrotina continua propagando.
- Logs não registram conteúdo do usuário. No máximo registram o tipo do erro, ou nada.
- `startFlexible` sem `check()` anterior com `Available` devolve `false`.
