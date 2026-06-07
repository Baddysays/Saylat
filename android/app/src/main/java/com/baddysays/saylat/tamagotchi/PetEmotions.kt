package com.baddysays.saylat.tamagotchi

import androidx.compose.ui.graphics.Color

enum class PetEmotion(
    val label: String,
    val category: EmotionCategory,
    val eyeType: EyeType,
    val mouthType: MouthType,
    val hasCheeks: Boolean,
    val browType: BrowType,
    val bodyTint: Color?,
    val accessories: List<AccessoryType>,
    val dialogCategory: DialogCategory,
    val loopFrames: Int = 120,
) {
    IDLE(
        "Обычный", EmotionCategory.BASE,
        EyeType.NORMAL, MouthType.SMILE, false, BrowType.NORMAL,
        null, emptyList(), DialogCategory.IDLE
    ),
    HAPPY(
        "Счастливый", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.SMILE, true, BrowType.NORMAL,
        null, emptyList(), DialogCategory.SUCCESS
    ),
    ECSTATIC(
        "В восторге", EmotionCategory.POSITIVE,
        EyeType.BIG, MouthType.BIG_SMILE, true, BrowType.NORMAL,
        Color(0xFFE8C070), listOf(AccessoryType.STARS_FLOAT), DialogCategory.EXCITED, 90
    ),
    LOADING(
        "Загружает", EmotionCategory.WORK,
        EyeType.LOADING, MouthType.FLAT, false, BrowType.NORMAL,
        null, emptyList(), DialogCategory.LOADING, 60
    ),
    DONE(
        "Готово!", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.GRIN, true, BrowType.NORMAL,
        Color(0xFFB8E8A0), listOf(AccessoryType.STARS_FLOAT), DialogCategory.SUCCESS
    ),
    ERROR(
        "Ошибка", EmotionCategory.NEGATIVE,
        EyeType.X_EYES, MouthType.FROWN, false, BrowType.ANGRY,
        Color(0xFFE89090), emptyList(), DialogCategory.ERROR
    ),
    THINKING(
        "Думает", EmotionCategory.BASE,
        EyeType.LOOKING_UP, MouthType.TINY, false, BrowType.NORMAL,
        null, listOf(AccessoryType.THOUGHT_BUBBLE), DialogCategory.PHILOSOPHICAL
    ),
    SLEEPING(
        "Спит", EmotionCategory.REST,
        EyeType.CLOSED, MouthType.FLAT, false, BrowType.NORMAL,
        Color(0xFFD0D8F0), listOf(AccessoryType.ZZZ), DialogCategory.SLEEPING, 180
    ),
    SURPRISED(
        "Удивлён", EmotionCategory.BASE,
        EyeType.BIG, MouthType.O_MOUTH, false, BrowType.NORMAL,
        null, listOf(AccessoryType.FLASH), DialogCategory.EXCITED
    ),
    IN_LOVE(
        "Влюблён", EmotionCategory.POSITIVE,
        EyeType.HEART, MouthType.SMILE, true, BrowType.NORMAL,
        Color(0xFFF0C0C0), listOf(AccessoryType.HEARTS_FLOAT), DialogCategory.IDLE, 90
    ),
    COOL(
        "Крутой", EmotionCategory.BASE,
        EyeType.SUNGLASSES, MouthType.SMIRK, false, BrowType.NONE,
        null, emptyList(), DialogCategory.FUN
    ),
    READING(
        "Читает", EmotionCategory.WORK,
        EyeType.GLASSES, MouthType.TINY, false, BrowType.FOCUSED,
        null, emptyList(), DialogCategory.TIPS
    ),
    CONFUSED(
        "Запутался", EmotionCategory.NEGATIVE,
        EyeType.SPIRAL, MouthType.WAVY, false, BrowType.NORMAL,
        null, listOf(AccessoryType.QUESTION_MARK), DialogCategory.ERROR
    ),
    ANGRY(
        "Злой", EmotionCategory.NEGATIVE,
        EyeType.ANGRY, MouthType.FROWN, false, BrowType.ANGRY,
        Color(0xFFF08060), listOf(AccessoryType.STEAM), DialogCategory.ERROR
    ),
    SHY(
        "Стесняется", EmotionCategory.BASE,
        EyeType.HAPPY, MouthType.TINY, true, BrowType.NORMAL,
        Color(0xFFF0C8C8), emptyList(), DialogCategory.IDLE
    ),
    WINKING(
        "Подмигивает", EmotionCategory.POSITIVE,
        EyeType.WINK, MouthType.SMILE, true, BrowType.NORMAL,
        null, emptyList(), DialogCategory.SUCCESS
    ),
    CRYING(
        "Плачет", EmotionCategory.NEGATIVE,
        EyeType.SAD, MouthType.FROWN, false, BrowType.WORRIED,
        Color(0xFFB8C8E8), listOf(AccessoryType.TEARS), DialogCategory.ERROR, 80
    ),
    DANCING(
        "Танцует", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.GRIN, true, BrowType.NORMAL,
        Color(0xFFD8C8F8), listOf(AccessoryType.MUSIC_NOTES), DialogCategory.EXCITED, 80
    ),
    EATING(
        "Ест", EmotionCategory.BASE,
        EyeType.HAPPY, MouthType.EATING, true, BrowType.NORMAL,
        null, emptyList(), DialogCategory.IDLE
    ),
    CAFFEINATED(
        "На кофеине", EmotionCategory.POSITIVE,
        EyeType.BIG, MouthType.BIG_SMILE, false, BrowType.NORMAL,
        Color(0xFFF8D870), listOf(AccessoryType.COFFEE_STEAM, AccessoryType.SPEED_LINES),
        DialogCategory.EXCITED, 40
    ),
    MUSIC(
        "Музыка", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.SMILE, true, BrowType.NORMAL,
        Color(0xFFC8D0F8), listOf(AccessoryType.HEADPHONES, AccessoryType.MUSIC_NOTES),
        DialogCategory.IDLE
    ),
    STAR_STRUCK(
        "Потрясён", EmotionCategory.POSITIVE,
        EyeType.STAR, MouthType.O_MOUTH, true, BrowType.NORMAL,
        null, listOf(AccessoryType.STARS_FLOAT), DialogCategory.EXCITED
    ),
    DIZZY(
        "Кружится", EmotionCategory.NEGATIVE,
        EyeType.SPIRAL, MouthType.WAVY, false, BrowType.NORMAL,
        Color(0xFFE8D8F8), listOf(AccessoryType.DIZZY_STARS), DialogCategory.ERROR, 60
    ),
    SICK(
        "Заболел", EmotionCategory.NEGATIVE,
        EyeType.TIRED, MouthType.WAVY, false, BrowType.WORRIED,
        Color(0xFFD8F0D8), listOf(AccessoryType.SWEAT_DROPS), DialogCategory.ERROR
    ),
    STRONG(
        "Сильный", EmotionCategory.POSITIVE,
        EyeType.DETERMINED, MouthType.GRIN, false, BrowType.FOCUSED,
        Color(0xFFF8D860), emptyList(), DialogCategory.VICTORY
    ),
    DEEP_THINK(
        "Глубокие мысли", EmotionCategory.WORK,
        EyeType.LOOKING_UP, MouthType.TINY, false, BrowType.NORMAL,
        null, listOf(AccessoryType.THOUGHT_BUBBLE, AccessoryType.SWEAT_DROPS),
        DialogCategory.PHILOSOPHICAL
    ),
    YAWNING(
        "Зевает", EmotionCategory.REST,
        EyeType.CLOSED, MouthType.YAWN, false, BrowType.NORMAL,
        Color(0xFFE8E0D8), listOf(AccessoryType.ZZZ), DialogCategory.SLEEPING
    ),
    PARTY(
        "Праздник", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.BIG_SMILE, true, BrowType.NORMAL,
        Color(0xFFF8D8A0), listOf(AccessoryType.PARTY_HAT, AccessoryType.CONFETTI),
        DialogCategory.VICTORY, 60
    ),
    VICTORY(
        "Победа", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.GRIN, true, BrowType.NORMAL,
        Color(0xFFD8F8C0), listOf(AccessoryType.CROWN, AccessoryType.STARS_FLOAT),
        DialogCategory.VICTORY
    ),
    CODING(
        "Пишет код", EmotionCategory.WORK,
        EyeType.FOCUSED, MouthType.TINY, false, BrowType.FOCUSED,
        Color(0xFFC0E8C0), listOf(AccessoryType.CODE_GLOW), DialogCategory.FUN
    ),
    SEARCHING(
        "Ищет", EmotionCategory.WORK,
        EyeType.SEARCHING, MouthType.SMILE, false, BrowType.NORMAL,
        null, emptyList(), DialogCategory.LOADING
    ),
    WARNING(
        "Предупреждает", EmotionCategory.WORK,
        EyeType.WORRIED, MouthType.WAVY, false, BrowType.WORRIED,
        Color(0xFFF8E898), listOf(AccessoryType.FLASH), DialogCategory.ERROR
    ),
    DOWNLOADING(
        "Скачивает", EmotionCategory.WORK,
        EyeType.LOADING, MouthType.DETERMINED, false, BrowType.FOCUSED,
        Color(0xFFC8E8F8), listOf(AccessoryType.PROGRESS_BAR), DialogCategory.LOADING, 120
    ),
    SNEAKY(
        "Хитрит", EmotionCategory.BASE,
        EyeType.SIDE_EYE, MouthType.SMIRK, false, BrowType.NORMAL,
        null, emptyList(), DialogCategory.FUN
    ),
    BORED(
        "Скучает", EmotionCategory.BASE,
        EyeType.TIRED, MouthType.FLAT, false, BrowType.NORMAL,
        Color(0xFFD8D8D8), emptyList(), DialogCategory.BORED, 200
    ),
    SMUG(
        "Самодовольный", EmotionCategory.BASE,
        EyeType.CLOSED, MouthType.SMIRK, false, BrowType.NONE,
        null, emptyList(), DialogCategory.FUN
    ),
    WORRIED(
        "Беспокоится", EmotionCategory.NEGATIVE,
        EyeType.WORRIED, MouthType.FROWN, false, BrowType.WORRIED,
        null, listOf(AccessoryType.SWEAT_DROPS), DialogCategory.ERROR
    ),
    RELIEVED(
        "Облегчение", EmotionCategory.POSITIVE,
        EyeType.HAPPY, MouthType.SMILE, true, BrowType.NORMAL,
        Color(0xFFD8F8D8), emptyList(), DialogCategory.SUCCESS
    ),
    FREEZING(
        "Замерзает", EmotionCategory.NEGATIVE,
        EyeType.NORMAL, MouthType.CHATTERING, false, BrowType.NORMAL,
        Color(0xFFB0C8E8), listOf(AccessoryType.ICE_CRYSTALS), DialogCategory.ERROR, 30
    ),
    SWEATING(
        "Перегрелся", EmotionCategory.NEGATIVE,
        EyeType.TIRED, MouthType.FLAT, true, BrowType.WORRIED,
        Color(0xFFF09090), listOf(AccessoryType.SWEAT_DROPS), DialogCategory.LOADING
    ),
    HYPER(
        "Гиперактивный", EmotionCategory.POSITIVE,
        EyeType.BIG, MouthType.GRIN, true, BrowType.NORMAL,
        Color(0xFFF0D070), listOf(AccessoryType.SPEED_LINES, AccessoryType.STARS_FLOAT),
        DialogCategory.EXCITED, 30
    ),
    GLITCH(
        "Глитч", EmotionCategory.NEGATIVE,
        EyeType.X_EYES, MouthType.FLAT, false, BrowType.NONE,
        Color(0xFFA0D0A0), listOf(AccessoryType.GLITCH_BARS), DialogCategory.ERROR, 15
    ),
    RAINBOW(
        "Радуга", EmotionCategory.POSITIVE,
        EyeType.RAINBOW, MouthType.BIG_SMILE, true, BrowType.NONE,
        null, listOf(AccessoryType.RAINBOW_ARC, AccessoryType.UNICORN_HORN),
        DialogCategory.VICTORY
    ),
    DEAD(
        "Умер", EmotionCategory.NEGATIVE,
        EyeType.X_EYES, MouthType.DEAD, false, BrowType.NONE,
        Color(0xFFD8D8D8), emptyList(), DialogCategory.ERROR
    ),
    CELEBRATING(
        "Празднует", EmotionCategory.POSITIVE,
        EyeType.STAR, MouthType.BIG_SMILE, true, BrowType.NORMAL,
        Color(0xFFF8E8A8), listOf(AccessoryType.FIREWORKS, AccessoryType.CONFETTI),
        DialogCategory.VICTORY, 60
    ),
}

enum class EmotionCategory(val label: String) {
    BASE("Базовые"), POSITIVE("Позитивные"),
    NEGATIVE("Негативные"), WORK("Работа"), REST("Отдых")
}

enum class EyeType {
    NORMAL, HAPPY, SAD, BIG, CLOSED, X_EYES, HEART, STAR,
    SPIRAL, SIDE_EYE, TIRED, ANGRY, WORRIED, RAINBOW,
    LOADING, FOCUSED, LOOKING_UP, SUNGLASSES, GLASSES,
    SEARCHING, DETERMINED, WINK
}

enum class MouthType {
    SMILE, TINY, BIG_SMILE, GRIN, FROWN, O_MOUTH, FLAT,
    WAVY, YAWN, TONGUE, SMIRK, EATING, CHATTERING, DEAD, DETERMINED
}

enum class BrowType { NORMAL, ANGRY, WORRIED, FOCUSED, NONE }

enum class AccessoryType {
    ZZZ, TEARS, HEARTS_FLOAT, STARS_FLOAT, MUSIC_NOTES,
    COFFEE_STEAM, SPEED_LINES, THOUGHT_BUBBLE, SWEAT_DROPS,
    CONFETTI, FIREWORKS, PROGRESS_BAR, GLITCH_BARS,
    RAINBOW_ARC, UNICORN_HORN, ICE_CRYSTALS, FLASH,
    CODE_GLOW, DIZZY_STARS, QUESTION_MARK, PARTY_HAT,
    HEADPHONES, CROWN, STEAM
}
