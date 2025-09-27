# Tco2Display

Minimal Jetpack Compose app that calls the Intangles API every 5 seconds and displays only the **tCO₂ saved** in elegant white text on a black background.

## CI Build (GitHub Actions)
1. Create **Actions Secret** `INTANGLES_TOKEN` in your repo (Settings → Secrets and variables → Actions).
2. Push to `main`. The workflow builds and uploads **app-debug.apk** as an artifact.

## Local build
- Put `INTANGLES_TOKEN=your_token` into your **user** `~/.gradle/gradle.properties`.
- Run: `./gradlew assembleDebug` (if you add the wrapper) or `gradle assembleDebug` with a local Gradle install.
