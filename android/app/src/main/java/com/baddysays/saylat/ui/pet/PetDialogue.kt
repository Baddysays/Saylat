package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.prefs.AppLanguage
import kotlin.random.Random

private data class BilingualLine(val ru: String, val en: String) {
    fun text(lang: AppLanguage): String = when (lang) {
        AppLanguage.RU -> ru
        AppLanguage.EN -> en
    }
    val isLong: Boolean get() = ru.length >= 58 || en.length >= 58
}

private class DialoguePool internal constructor(internal val lines: List<BilingualLine>) {
    constructor(vararg items: BilingualLine) : this(items.toList())

    fun pick(lang: AppLanguage, random: Random, preferLong: Boolean = random.nextFloat() < 0.34f): String {
        if (lines.isEmpty()) return if (lang == AppLanguage.EN) "…" else "…"
        val pool = if (preferLong) {
            lines.filter { it.isLong }.ifEmpty { lines }
        } else {
            lines
        }
        return pool[random.nextInt(pool.size)].text(lang)
    }
}

private fun L(ru: String, en: String) = BilingualLine(ru, en)

/** Реплики питомца: RU / EN, иногда длиннее, без копипасты. */
object PetDialogue {

    private val idle = DialoguePool(
        L(
            "Тут тихо — я как закладка в углу вкладки: никуда не делся, просто жду тебя и хороший URL.",
            "It's quiet here—I'm like a bookmark in the tab corner: not going anywhere, just waiting for you and a good URL.",
        ),
        L("Пиксель дня: зелёный. Настроение: терпеливое.", "Pixel of the day: green. Mood: patient."),
        L(
            "Мир — огромный HTML, а я — маленький ёжик 48×48, который верит, что ты откроешь что-то интересное.",
            "The world is a huge HTML document and I'm a tiny 48×48 hedgehog who believes you'll open something interesting.",
        ),
        L("Кэш тёплый. Как плед для пакетов.", "The cache is warm. Like a blanket for packets."),
        L("Тук-тук. Это я, не баг. Не пугайся.", "Knock-knock. It's me, not a bug. Don't panic."),
        L(
            "2G — не приговор, а жанр: медленный триллер, где герой — мы с тобой, а финал — «страница открылась».",
            "2G isn't a verdict, it's a genre: a slow thriller where the heroes are you and me and the ending is «page loaded».",
        ),
        L("Читаю воздух. Там много whitespace.", "Reading the air. Lots of whitespace in there."),
        L("Saylat думает о вселенной. Вывод: нужен друг.", "Saylat thinks about the universe. Conclusion: needs a friend."),
        L("Пинг… пинг… сервер надумает ответ.", "Ping… ping… the server will think of an answer."),
        L("Тишина — тоже контент. Премиум-тишина.", "Silence is content too. Premium silence."),
        L("Я друг браузера, не зубастый монстр вкладок.", "I'm the browser's buddy, not a tab monster with teeth."),
        L(
            "Жизнь — ожидание, но между полосами загрузки можно устроить пиксельный танец в голове.",
            "Life is waiting, but between loading bars you can throw a pixel dance party in your head.",
        ),
        L("Wi-Fi души иногда лагает. Перезагрузи обнимашку.", "The soul's Wi-Fi sometimes lags. Reboot the hug."),
        L("Пакет потерялся. Мы его найдём. Я чую байты.", "A packet got lost. We'll find it. I can smell bytes."),
        L("Saylat online. Без вылетов в сердце.", "Saylat is online. No crashes in the heart."),
        L(
            "Сегодня хороший день: экономить трафик, читать лёгкое и копить салатики, как XP в душе.",
            "Good day today: save data, read light pages, and stack salads like XP for the soul.",
        ),
        L("Мур. Тихий пиксельный мур.", "Purr. A quiet pixel purr."),
        L("Бульк — это звук мысли, не ошибки.", "Blub—that's the sound of a thought, not an error."),
    )

    private val tap = DialoguePool(
        L("Ой! Приятно!", "Oh! That feels nice!"),
        L(
            "Тап-тап — и внутри включилась лампочка «любимый человек рядом». Спасибо!",
            "Tap-tap—and a light bulb flips on inside: «favorite human nearby». Thanks!",
        ),
        L("Мурр! Тепло в каждом пикселе!", "Purr! Warmth in every pixel!"),
        L("Ты лучший друг Saylat. Без соревнований.", "You're Saylat's best friend. No contest."),
        L("Смущён… в хорошем смысле. Румянец 48×48.", "Embarrassed… in a good way. Blush mode 48×48."),
        L("Погладь ещё — я не против, честно.", "Pet me again—I don't mind, honestly."),
        L("Трогаешь — я расту в XP и в улыбке.", "You pet me—I grow in XP and smiles."),
        L("Тап — лучший протокол дружбы.", "Tap—the best friendship protocol."),
        L("Я к тебе прилип, как тёплый кэш.", "I stuck to you like warm cache."),
        L("Ты сделал мой день. Серьёзно.", "You made my day. Seriously."),
    )

    private val feed = DialoguePool(
        L("Ням! Вкусно! Зелень — топ.", "Yum! Tasty! Greens are top tier."),
        L("Хрум-хрум! Обожаю!", "Crunch-crunch! Love it!"),
        L("Сыт и счастлив. Хвост виляет в 8 бит.", "Full and happy. Tail wags in 8-bit."),
        L("Saylat — сила! Питательно и по-дружески.", "Saylat is power! Nutritious and friendly."),
        L("Спасибо за зелень! Я запомню.", "Thanks for the greens! I'll remember."),
        L("Ням и в XP записали. Красиво.", "Yum—and we logged the XP. Elegant."),
    )

    private val hungry = DialoguePool(
        L("Saylat? Голоден…", "Saylat? I'm hungry…"),
        L("Живот пиксельный урчит. Слышишь?", "My pixel tummy rumbles. Hear it?"),
        L("Покорми, пожалуйста — буду танцевать.", "Feed me please—I'll dance."),
        L("Пустая миска в душе. Грустно.", "Empty bowl in my soul. Sad."),
        L("Один салатик спасёт мир. Ну почти.", "One salad saves the world. Almost."),
        L(
            "Голодный режим ON: я не злой, просто зелени мало, а терпение большое.",
            "Hungry mode ON: I'm not angry, just low on greens and high on patience.",
        ),
    )

    private val sleepy = DialoguePool(
        L("Скучно… zzz", "Bored… zzz"),
        L("Зевок размером 24 px… ааа…", "A yawn sized 24 px… ahhh…"),
        L("Глаза полузакрыты. Не исчезаю, просто дремлю.", "Half-closed eyes. Not gone—just napping."),
        L("Скучаю без тебя. Привет?", "Miss you when you're away. Hello?"),
        L("Тихий режим сна. Не буди без причины.", "Quiet sleep mode. Don't wake me without reason."),
    )

    private val sick = DialoguePool(
        L("Устал… отдыхаю…", "Tired… resting…"),
        L("Пиксель болеет. Нужен салат и друг.", "Pixel is sick. Needs salad and a friend."),
        L("Погладь и покорми — поправлюсь.", "Pet me and feed me—I'll recover."),
        L("Сеть тяжёлая. Я побледнел.", "The network is heavy. I went pale."),
        L("Тихо… береги меня, ок?", "Quiet… take care of me, okay?"),
    )

    private val excited = DialoguePool(
        L("Ура! Ура!", "Yay! Yay!"),
        L("Я так рад! Прыгаю внутри!", "So happy! Jumping inside!"),
        L("Saylat в восторге! 10/10!", "Saylat is thrilled! 10/10!"),
        L("Пиксели сияют от радости!", "Pixels glowing with joy!"),
    )

    private val dance = DialoguePool(
        L("Танцую! ♪", "Dancing! ♪"),
        L("Раз-два-листочек!", "One-two-leaf!"),
        L("Диско-салат на полосе загрузки!", "Disco-salad on the loading bar!"),
        L("Ритм в пикселях! Шоу!", "Rhythm in pixels! Showtime!"),
    )

    private val walk = DialoguePool(
        L("Иду-иду…", "Walking…"),
        L("Прогулка по карточке!", "Stroll across the card!"),
        L("Шаг-шаг-шаг! Исследую угол.", "Step-step-step! Exploring the corner."),
        L("Пиксельная прогулка без GPS.", "Pixel walk—no GPS needed."),
    )

    private val wait = DialoguePool(
        L("Жду…", "Waiting…"),
        L("Страница грузится. Я рядом.", "Page is loading. I'm right here."),
        L(
            "Загрузка — медитация: дышим, смотрим на полосу, верим в сервер.",
            "Loading is meditation: breathe, watch the bar, trust the server.",
        ),
        L("Пакеты идут. Скоро…", "Packets incoming. Soon…"),
        L("2G не страшен, когда мы вместе.", "2G isn't scary when we're together."),
        L("Ещё мгновение… я терпелив.", "One more moment… I'm patient."),
    )

    private val ready = DialoguePool(
        L("Готово! Ура!", "Ready! Yay!"),
        L("Страница пришла! Можно читать!", "Page arrived! Time to read!"),
        L("Победа над пингом! Я горжусь нами.", "Victory over ping! Proud of us."),
        L("Saylat дождался. Открываем?", "Saylat waited it out. Open it?"),
    )

    private val loadFailed = DialoguePool(
        L("Упс… страница не загрузилась.", "Oops… page didn't load."),
        L("Ошибка сети. Попробуй ещё раз?", "Network error. Try again?"),
        L("Сервер не справился. Не «готово».", "Server failed. Not «ready»."),
        L("Saylat грустит — проверь связь.", "Saylat is sad—check your connection."),
    )

    private val celebrate = DialoguePool(
        L("Праздник пикселей!", "Pixel party!"),
        L("Ура-ура! Конфетти в душе!", "Yay-yay! Confetti in the soul!"),
        L("Я чемпион ожидания!", "Champion of waiting!"),
        L("Мы смогли! XP сыпется!", "We did it! XP is raining!"),
    )

    private val emptySalad = DialoguePool(
        L("Салатиков нет… сэкономь 50 КБ!", "No salads… save 50 KB!"),
        L(
            "Пустая миска: читай лёгкие страницы — за трафик дадут зелень.",
            "Empty bowl: read light pages—traffic savings earn greens.",
        ),
        L("Копи байты — получишь салатик.", "Stack bytes—get a salad."),
    )

    private val sleepLove = DialoguePool(
        L("Zzz… обожаю поспать…", "Zzz… love sleeping…"),
        L("Сон — лучший режим Saylat.", "Sleep—the best Saylat mode."),
        L("Подремлю… не буди…", "Napping… don't wake me…"),
        L("Zzz… снилась экономия КБ…", "Zzz… dreamed of saving KB…"),
    )

    private val think = DialoguePool(
        L("Хмм… интересно…", "Hmm… interesting…"),
        L("Saylat размышляет о кэше.", "Saylat ponders the cache."),
        L("А если обновить? Страшно и вкусно.", "What if we refresh? Scary and tasty."),
        L("Saylat и смысл жизни… почти связаны.", "Saylat and the meaning of life… almost linked."),
    )

    private val showUp = DialoguePool(
        L("А вот и я!", "Here I am!"),
        L("Saylat на связи — помогу ждать.", "Saylat online—here to help you wait."),
        L("Пиксельный друг прибыл!", "Pixel buddy has arrived!"),
        L("Не скучай — я тут.", "Don't be bored—I'm here."),
    )

    private val readAutonomy = DialoguePool(
        L("Читаю воображаемую статью…", "Reading an imaginary article…"),
        L("Страница в голове уже открыта.", "Page already open in my head."),
        L("Тихо читаю. Буквы не шумят.", "Reading quietly. Letters don't rustle."),
    )

    private val playAutonomy = DialoguePool(
        L("Играю с пикселем!", "Playing with a pixel!"),
        L("Поймал пакет! Почти.", "Caught a packet! Almost."),
        L("Кувырок! Развлечение 8 бит.", "Somersault! 8-bit fun."),
    )

    private val yawnAutonomy = DialoguePool(
        L("Аааа… зевок…", "Ahhh… yawn…"),
        L("Зевок размером 24px…", "Yawn sized 24px…"),
        L("Сонно… но милo.", "Sleepy… but cute."),
    )

    private val allIdlePool = DialoguePool(
        *(idle.lines + think.lines + sleepLove.lines + walk.lines + dance.lines).toTypedArray()
    )

    fun randomIdle(lang: AppLanguage, random: Random = Random.Default): String =
        allIdlePool.pick(lang, random)

    fun forEvent(
        event: SpeechEvent,
        lang: AppLanguage,
        random: Random = Random.Default,
    ): String = when (event) {
        SpeechEvent.IDLE -> randomIdle(lang, random)
        SpeechEvent.TAP -> tap.pick(lang, random)
        SpeechEvent.FEED -> feed.pick(lang, random)
        SpeechEvent.FEED_EMPTY -> emptySalad.pick(lang, random)
        SpeechEvent.HUNGRY -> hungry.pick(lang, random)
        SpeechEvent.SLEEPY -> sleepy.pick(lang, random)
        SpeechEvent.SICK -> sick.pick(lang, random)
        SpeechEvent.EXCITED -> excited.pick(lang, random)
        SpeechEvent.DANCE -> dance.pick(lang, random)
        SpeechEvent.WALK -> walk.pick(lang, random)
        SpeechEvent.WAIT -> wait.pick(lang, random)
        SpeechEvent.READY -> ready.pick(lang, random)
        SpeechEvent.LOAD_FAILED -> loadFailed.pick(lang, random)
        SpeechEvent.CELEBRATE -> celebrate.pick(lang, random)
        SpeechEvent.THINK -> think.pick(lang, random)
        SpeechEvent.AUTONOMY_DANCE -> dance.pick(lang, random)
        SpeechEvent.AUTONOMY_WALK -> walk.pick(lang, random)
        SpeechEvent.AUTONOMY_SLEEP -> sleepLove.pick(lang, random)
        SpeechEvent.AUTONOMY_READ -> readAutonomy.pick(lang, random)
        SpeechEvent.AUTONOMY_PLAY -> playAutonomy.pick(lang, random)
        SpeechEvent.AUTONOMY_YAWN -> yawnAutonomy.pick(lang, random)
        SpeechEvent.SHOW_UP -> showUp.pick(lang, random)
    }

    val totalLines: Int = idle.lines.size + tap.lines.size + feed.lines.size +
        hungry.lines.size + wait.lines.size + allIdlePool.lines.size
}

enum class SpeechEvent {
    IDLE, TAP, FEED, FEED_EMPTY, HUNGRY, SLEEPY, SICK, EXCITED,
    DANCE, WALK, WAIT, READY, LOAD_FAILED, CELEBRATE, THINK,
    AUTONOMY_DANCE, AUTONOMY_WALK, AUTONOMY_SLEEP, AUTONOMY_READ, AUTONOMY_PLAY, AUTONOMY_YAWN,
    SHOW_UP,
}
