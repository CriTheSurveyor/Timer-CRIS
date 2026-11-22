# Tabata Timer

Timer per allenamenti a intervalli (Android, Kotlin + Jetpack Compose), con grafica in stile Apple Liquid Glass.

- Allenamenti salvati con nome: crea, rinomina, modifica, elimina
- Preparazione, lavoro, riposo, cicli, serie, recupero tra serie, defaticamento
- Beep agli ultimi 3 secondi e a ogni cambio fase, schermo sempre acceso durante l'allenamento
- Pausa, intervallo precedente/successivo

## APK e aggiornamenti
Ogni push su `main` compila l'APK su GitHub Actions e lo pubblica nelle **Releases** come `build-N`.
Prima installazione: scarica `timer-cris.apk` dall'ultima release sul telefono.
Poi l'app controlla le nuove release: in home compare **Aggiorna alla build N**, un tocco scarica l'APK e lo installi sopra.

La firma (uguale su ogni build, serve per aggiornare) viene dai segreti del repo `KEYSTORE_B64` e `KEYSTORE_PASSWORD`.

Build locale: `gradle assembleDebug` (JDK 17 + Android SDK).
