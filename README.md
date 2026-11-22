# Tabata Timer

Timer per allenamenti a intervalli (Android, Kotlin + Jetpack Compose), con grafica in stile Apple Liquid Glass.

- Allenamenti salvati con nome: crea, rinomina, modifica, elimina
- Preparazione, lavoro, riposo, cicli, serie, recupero tra serie, defaticamento
- Beep agli ultimi 3 secondi e a ogni cambio fase, schermo sempre acceso durante l'allenamento
- Pausa, intervallo precedente/successivo

## APK
Ogni push compila l'APK su GitHub Actions (tab **Actions** → ultimo run → artifact `tabata-apk`).
Un tag `v*` (es. `v1.0`) lo pubblica anche nelle **Releases**, scaricabile dal telefono.

Build locale: `gradle assembleDebug` (JDK 17 + Android SDK).
