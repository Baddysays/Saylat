package com.baddysays.saylat.ui.strings

import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.prefs.ReaderMode
import com.baddysays.saylat.ui.PetMood
import com.baddysays.saylat.ui.QuickSpeedMode
import com.baddysays.saylat.ui.SettingsTab

/** Строки интерфейса (RU / EN). */
object SaylatStrings {

    fun settingsTab(tab: SettingsTab, lang: AppLanguage): String = when (tab) {
        SettingsTab.GENERAL -> when (lang) {
            AppLanguage.RU -> "Основные"
            AppLanguage.EN -> "General"
        }
        SettingsTab.PET -> when (lang) {
            AppLanguage.RU -> "Питомец"
            AppLanguage.EN -> "Pet"
        }
        SettingsTab.NETWORK -> when (lang) {
            AppLanguage.RU -> "Сеть"
            AppLanguage.EN -> "Network"
        }
        SettingsTab.READER -> when (lang) {
            AppLanguage.RU -> "Чтение"
            AppLanguage.EN -> "Reader"
        }
        SettingsTab.SERVICES -> when (lang) {
            AppLanguage.RU -> "Сервисы"
            AppLanguage.EN -> "Services"
        }
    }

    fun settingsLanguageTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Язык"
        AppLanguage.EN -> "Language"
    }

    fun settingsLanguageHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Меняет подписи в приложении и реплики Saylat."
        AppLanguage.EN -> "Changes app labels and Saylat dialogue."
    }

    fun settingsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Настройки"
        AppLanguage.EN -> "Settings"
    }

    fun settingsBack(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Назад"
        AppLanguage.EN -> "Back"
    }

    fun settingsHubGeneralSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Язык, тема, кэш, обновления"
        AppLanguage.EN -> "Language, theme, cache, updates"
    }

    fun settingsHubPetSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Магазин, имя, вкл/выкл, при загрузке"
        AppLanguage.EN -> "Shop, name, on/off, while loading"
    }

    fun settingsHubNetworkSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Скорость, сервер, тест сети"
        AppLanguage.EN -> "Speed, server, network test"
    }

    fun settingsHubReaderSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Режим чтения, умная вёрстка"
        AppLanguage.EN -> "Reader mode, smart layout"
    }

    fun settingsHubServicesSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Поиск, перевод, Telegram"
        AppLanguage.EN -> "Search, translate, Telegram"
    }

    fun settingsAppearance(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Оформление"
        AppLanguage.EN -> "Appearance"
    }

    fun settingsApp(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Приложение"
        AppLanguage.EN -> "App"
    }

    fun settingsCache(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Кэш"
        AppLanguage.EN -> "Cache"
    }

    fun collapse(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Свернуть"
        AppLanguage.EN -> "Collapse"
    }

    fun tapToOpenPet(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Тапни — открыть"
        AppLanguage.EN -> "Tap to open"
    }

    fun pageReadyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Готово"
        AppLanguage.EN -> "Ready"
    }

    fun pageReadySubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Страница загружена"
        AppLanguage.EN -> "Page loaded"
    }

    fun pageLoadFailedTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Ошибка"
        AppLanguage.EN -> "Error"
    }

    fun pageLoadFailedSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Страница не загрузилась"
        AppLanguage.EN -> "Page failed to load"
    }

    fun readerLoadingStrips(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Рисуем полосы…"
        AppLanguage.EN -> "Rendering strips…"
    }

    fun readerLoadingPage(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Загружаем страницу…"
        AppLanguage.EN -> "Loading page…"
    }

    fun smartLayoutNeedsTextMode(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Умная вёрстка — только в режиме «Экономия текста»"
        AppLanguage.EN -> "Smart layout works in «Text savings» mode only"
    }

    fun smartLayoutUnavailableToast(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Умная вёрстка недоступна на этом устройстве"
        AppLanguage.EN -> "Smart layout isn't available on this device"
    }

    fun petSaladButton(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.RU -> "Салатик ×$count"
        AppLanguage.EN -> "Salad ×$count"
    }

    fun petPetButton(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Погладить"
        AppLanguage.EN -> "Pet"
    }

    fun petSaladHint(lang: AppLanguage, kbUntil: String) = when (lang) {
        AppLanguage.RU -> "Нужен салатик: сэкономь $kbUntil (50 КБ = 1 салатик)"
        AppLanguage.EN -> "Need a salad: save $kbUntil (50 KB = 1 salad)"
    }

    fun petSaladProgress(lang: AppLanguage, kbUntil: String) = when (lang) {
        AppLanguage.RU -> "Салатики с экономии трафика · до +1: $kbUntil"
        AppLanguage.EN -> "Salads from saved traffic · next in: $kbUntil"
    }

    fun loadingUrl(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Загрузка…"
        AppLanguage.EN -> "Loading…"
    }

    fun headerEggHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "яйцо · тап — характеристики"
        AppLanguage.EN -> "egg · tap for stats"
    }

    fun headerSleep(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "спит"
        AppLanguage.EN -> "sleeping"
    }

    fun headerDance(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "танцует"
        AppLanguage.EN -> "dancing"
    }

    fun headerRead(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "читает"
        AppLanguage.EN -> "reading"
    }

    fun moodLabel(mood: PetMood, lang: AppLanguage): String = when (mood) {
        PetMood.HAPPY -> when (lang) {
            AppLanguage.RU -> "Доволен"
            AppLanguage.EN -> "Happy"
        }
        PetMood.EXCITED -> when (lang) {
            AppLanguage.RU -> "Счастлив"
            AppLanguage.EN -> "Excited"
        }
        PetMood.HUNGRY -> when (lang) {
            AppLanguage.RU -> "Хочет салат"
            AppLanguage.EN -> "Hungry"
        }
        PetMood.SLEEPY -> when (lang) {
            AppLanguage.RU -> "Скучает"
            AppLanguage.EN -> "Bored"
        }
        PetMood.SICK -> when (lang) {
            AppLanguage.RU -> "Устал"
            AppLanguage.EN -> "Tired"
        }
    }

    fun statHp(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "HP"
        AppLanguage.EN -> "HP"
    }

    fun statFood(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Еда"
        AppLanguage.EN -> "Food"
    }

    fun statJoy(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Рад"
        AppLanguage.EN -> "Joy"
    }

    fun growthLine(
        lang: AppLanguage,
        eaten: String,
        nextTitle: String,
        untilNext: String,
    ) = when (lang) {
        AppLanguage.RU -> "Рост: $eaten / 100 МБ · до $nextTitle: $untilNext"
        AppLanguage.EN -> "Growth: $eaten / 100 MB · to $nextTitle: $untilNext"
    }

    fun growthMax(lang: AppLanguage, eaten: String) = when (lang) {
        AppLanguage.RU -> "Максимальный рост · $eaten"
        AppLanguage.EN -> "Max growth · $eaten"
    }

    fun offlineBanner(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Нет интернета — доступен офлайн-кэш"
        AppLanguage.EN -> "Offline — cached pages still work"
    }

    fun homeEmptyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Пока пусто"
        AppLanguage.EN -> "Nothing here yet"
    }

    fun homeEmptyBody(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Закрепите страницу в читалке или откройте сайт — он попадёт в офлайн-кэш."
        AppLanguage.EN -> "Pin a page in the reader or open a site — it goes to offline cache."
    }

    fun homeSearchHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Поиск — в строке внизу экрана."
        AppLanguage.EN -> "Search using the bar at the bottom."
    }

    fun homePinsAndCache(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Закрепы и кэш"
        AppLanguage.EN -> "Pins and cache"
    }

    fun homeSmartLayoutNote(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Умная вёрстка недоступна на этом устройстве (мало RAM)."
        AppLanguage.EN -> "Smart layout isn't available on this device (low RAM)."
    }

    fun smartLayoutRamHint(lang: AppLanguage, ramMb: Long, minRamMb: Int) = when (lang) {
        AppLanguage.RU -> "Нужно ≥ $minRamMb МБ RAM (сейчас ~$ramMb МБ)"
        AppLanguage.EN -> "Requires ≥ $minRamMb MB RAM (now ~$ramMb MB)"
    }

    fun smartLayoutDefaultSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Второй проход на устройстве"
        AppLanguage.EN -> "Second layout pass on device"
    }

    fun petEggStatsHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Яйцо. Вылупление — тап по питомцу справа внизу при загрузке; в шапке — только характеристики."
        AppLanguage.EN -> "Egg. Hatch by tapping the pet (bottom-right) while a page loads; header shows stats only."
    }

    fun petFabContentDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Питомец — открыть"
        AppLanguage.EN -> "Pet — open"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Поиск или URL"
        AppLanguage.EN -> "Search or URL"
    }

    fun save(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Сохранить"
        AppLanguage.EN -> "Save"
    }

    fun settingsAppSection(lang: AppLanguage) = settingsApp(lang)

    fun settingsVersion(lang: AppLanguage, name: String, code: Int) = when (lang) {
        AppLanguage.RU -> "Версия $name ($code)"
        AppLanguage.EN -> "Version $name ($code)"
    }

    fun settingsCheckUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Проверить обновление"
        AppLanguage.EN -> "Check for update"
    }

    fun settingsUpdating(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Скачиваем…"
        AppLanguage.EN -> "Downloading…"
    }

    fun settingsCheckingUpdate(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Проверяем…"
        AppLanguage.EN -> "Checking…"
    }

    fun settingsCacheSection(lang: AppLanguage) = settingsCache(lang)

    fun settingsCacheEntries(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.RU -> "Записей: $count"
        AppLanguage.EN -> "Entries: $count"
    }

    fun settingsCacheStrips(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.RU -> "Полос: $count"
        AppLanguage.EN -> "Strip pages: $count"
    }

    fun settingsCacheSize(lang: AppLanguage, size: String) = when (lang) {
        AppLanguage.RU -> "Кэш: $size"
        AppLanguage.EN -> "Cache: $size"
    }

    fun settingsClearCache(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Очистить кэш"
        AppLanguage.EN -> "Clear cache"
    }

    fun settingsPetHeader(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Питомец в шапке"
        AppLanguage.EN -> "Pet in header"
    }

    fun settingsPetHeaderSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Анимации на главной; выкл — только логотип"
        AppLanguage.EN -> "Animations on home; off — logo only"
    }

    fun settingsPetSkipReady(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Без экрана «готово»"
        AppLanguage.EN -> "Skip «ready» screen"
    }

    fun settingsPetSkipReadySub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "При загрузке — сразу читать"
        AppLanguage.EN -> "Start reading as soon as loaded"
    }

    fun settingsPetShop(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Магазин"
        AppLanguage.EN -> "Shop"
    }

    fun settingsPetName(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Имя"
        AppLanguage.EN -> "Name"
    }

    fun settingsPetWallet(lang: AppLanguage, wallet: String, salads: Int) = when (lang) {
        AppLanguage.RU -> "Кошелёк $wallet · салатики $salads"
        AppLanguage.EN -> "Wallet $wallet · salads $salads"
    }

    fun settingsPetHatchHint(lang: AppLanguage) = petEggStatsHint(lang)

    fun settingsNetworkSection(lang: AppLanguage) = settingsTab(SettingsTab.NETWORK, lang)

    fun settingsSlowNetwork(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Медленная сеть"
        AppLanguage.EN -> "Slow network"
    }

    fun settingsSlowNetworkSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "2G / EDGE, длинные таймауты"
        AppLanguage.EN -> "2G / EDGE, long timeouts"
    }

    fun settingsLiteImages(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Лёгкие картинки"
        AppLanguage.EN -> "Lite images"
    }

    fun settingsLiteImagesSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Меньше трафика на изображения"
        AppLanguage.EN -> "Less traffic for images"
    }

    fun settingsServerOk(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Сервер отвечает"
        AppLanguage.EN -> "Server is up"
    }

    fun settingsServerDown(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Сервер недоступен"
        AppLanguage.EN -> "Server unreachable"
    }

    fun settingsServerChecking(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Проверяем…"
        AppLanguage.EN -> "Checking…"
    }

    fun settingsCustomServer(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Свой сервер"
        AppLanguage.EN -> "Custom server"
    }

    fun settingsCustomServerSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "VPS / свой прокси"
        AppLanguage.EN -> "VPS / your proxy"
    }

    fun settingsServerAddress(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Адрес"
        AppLanguage.EN -> "Address"
    }

    fun settingsReaderSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Режим чтения"
        AppLanguage.EN -> "Reading mode"
    }

    fun settingsSmartLayout(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Умная вёрстка"
        AppLanguage.EN -> "Smart layout"
    }

    fun settingsLoadStats(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Статистика загрузки"
        AppLanguage.EN -> "Load statistics"
    }

    fun settingsLoadStatsSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Вес страницы и экономия"
        AppLanguage.EN -> "Page size and savings"
    }

    fun settingsSearchSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Поиск"
        AppLanguage.EN -> "Search"
    }

    fun settingsClearHistory(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Очистить историю"
        AppLanguage.EN -> "Clear history"
    }

    fun settingsTranslateSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Перевод"
        AppLanguage.EN -> "Translate"
    }

    fun settingsAccountsSection(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Аккаунты"
        AppLanguage.EN -> "Accounts"
    }

    fun cacheKindText(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "текст"
        AppLanguage.EN -> "text"
    }

    fun cacheKindStrips(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "полосы"
        AppLanguage.EN -> "strips"
    }

    fun pinContentDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Закреп"
        AppLanguage.EN -> "Pinned"
    }

    fun cacheContentDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RU -> "Кэш"
        AppLanguage.EN -> "Cache"
    }

    fun readerModeLabel(mode: ReaderMode, lang: AppLanguage): String = when (mode) {
        ReaderMode.LAYOUT -> when (lang) {
            AppLanguage.RU -> "Экономия текста"
            AppLanguage.EN -> "Text savings"
        }
        ReaderMode.STRIPS -> when (lang) {
            AppLanguage.RU -> "Полосы"
            AppLanguage.EN -> "Strips"
        }
        ReaderMode.WEBVIEW -> when (lang) {
            AppLanguage.RU -> "Как на сайте"
            AppLanguage.EN -> "Like the site"
        }
        ReaderMode.AUTO -> when (lang) {
            AppLanguage.RU -> "Авто"
            AppLanguage.EN -> "Auto"
        }
        else -> mode.label
    }

    fun readerModeHint(mode: ReaderMode, lang: AppLanguage): String = when (mode) {
        ReaderMode.LAYOUT -> when (lang) {
            AppLanguage.RU -> ReaderMode.LAYOUT.hint
            AppLanguage.EN -> "Site becomes cards and text. Lightest reading mode."
        }
        ReaderMode.STRIPS -> when (lang) {
            AppLanguage.RU -> ReaderMode.STRIPS.hint
            AppLanguage.EN -> "Server renders long JPEG strips. Opera Mini style; can save to gallery."
        }
        ReaderMode.WEBVIEW -> when (lang) {
            AppLanguage.RU -> ReaderMode.WEBVIEW.hint
            AppLanguage.EN -> "Regular site in WebView. Use when exact layout matters."
        }
        ReaderMode.AUTO -> when (lang) {
            AppLanguage.RU -> ReaderMode.AUTO.hint
            AppLanguage.EN -> "Picks mode automatically: text on slow networks, strips on fast; WebView as fallback."
        }
        else -> mode.hint
    }

    fun quickSpeedTitle(mode: QuickSpeedMode, lang: AppLanguage): String = when (mode) {
        QuickSpeedMode.ECO -> when (lang) {
            AppLanguage.RU -> "Эко"
            AppLanguage.EN -> "Eco"
        }
        QuickSpeedMode.BALANCED -> when (lang) {
            AppLanguage.RU -> "Баланс"
            AppLanguage.EN -> "Balanced"
        }
        QuickSpeedMode.FAST -> when (lang) {
            AppLanguage.RU -> "Макс"
            AppLanguage.EN -> "Max"
        }
    }

    fun quickSpeedSubtitle(mode: QuickSpeedMode, lang: AppLanguage): String = when (mode) {
        QuickSpeedMode.ECO -> when (lang) {
            AppLanguage.RU -> "2G / минимум трафика"
            AppLanguage.EN -> "2G / minimum traffic"
        }
        QuickSpeedMode.BALANCED -> when (lang) {
            AppLanguage.RU -> "стабильно / умеренно"
            AppLanguage.EN -> "stable / moderate"
        }
        QuickSpeedMode.FAST -> when (lang) {
            AppLanguage.RU -> "Wi-Fi / быстро"
            AppLanguage.EN -> "Wi-Fi / fast"
        }
    }

    fun quickSpeedSummary(mode: QuickSpeedMode, lang: AppLanguage): String = when (mode) {
        QuickSpeedMode.ECO -> when (lang) {
            AppLanguage.RU -> QuickSpeedMode.ECO.summary
            AppLanguage.EN -> "Long timeouts, smallest images. Best for very weak networks."
        }
        QuickSpeedMode.BALANCED -> when (lang) {
            AppLanguage.RU -> QuickSpeedMode.BALANCED.summary
            AppLanguage.EN -> "Long timeouts without aggressive compression. Good for unstable mobile."
        }
        QuickSpeedMode.FAST -> when (lang) {
            AppLanguage.RU -> QuickSpeedMode.FAST.summary
            AppLanguage.EN -> "Normal timeouts and full images. Best on good Wi‑Fi."
        }
    }
}
