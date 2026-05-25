# Сборка Saylat (Android)

## Вариант A — Android Studio (рекомендуется)

1. Установить [Android Studio](https://developer.android.com/studio) (через winget: `Google.AndroidStudio`).
2. Первый запуск: мастер установит **Android SDK** и **JDK** (подтвердите лицензии).
3. **File → Open** → папка `android/` в этом репозитории.
4. Дождаться **Gradle Sync**.
5. **Run** ▶ на эмуляторе или устройстве.

Прокси на эмуляторе: `http://10.0.2.2:8787`

## Вариант B — командная строка

Требуется: JDK 17+, Android SDK (`ANDROID_HOME` или `%LOCALAPPDATA%\Android\Sdk`).

```powershell
cd C:\Users\Admin\Projects\thin-browser\android

# если SDK уже есть после Android Studio:
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
"sdk.dir=$($sdk -replace '\\','\\')" | Out-File -Encoding utf8 local.properties

.\gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

## Скрипт

Из корня репозитория:

```powershell
.\scripts\build-android.ps1
```

## Если сборка падает

- Нет SDK: откройте Android Studio один раз → SDK Manager → API 35.
- Нет Java: `winget install Microsoft.OpenJDK.17`
- Gradle wrapper: при первом запуске `gradlew` скачает Gradle автоматически.
