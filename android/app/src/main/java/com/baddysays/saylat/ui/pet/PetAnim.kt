package com.baddysays.saylat.ui.pet

/** Анимации питомца (кадры для эмоций и автономии). */
enum class PetAnim(val frames: Int) {
    IDLE(4),
    IDLE_BREATHE(4),
    BLINK(3),
    BOUNCE(4),
    WALK(6),
    WALK_SLOW(8),
    RUN(4),
    DANCE(8),
    DANCE_SPIN(6),
    DANCE_SHUFFLE(6),
    SLEEP(4),
    SLEEP_DEEP(6),
    YAWN(5),
    EAT(4),
    CHEW(4),
    THINK(5),
    READ(6),
    PLAY(6),
    CELEBRATE(6),
    WAVE(5),
    JUMP(4),
    STRETCH(5),
    SIT(4),
    LOVE(5),
    CRY(4),
    ANGRY(4),
    EXCITED(4),
    PEEK(4),
    LOOK_LEFT(3),
    LOOK_RIGHT(3),
    PLAY_BALL(8),
    CHAIR_ROCK(10),
    ;

    companion object {
        val autonomyPool = listOf(
            IDLE, IDLE_BREATHE, BLINK, WALK, WALK_SLOW, DANCE, DANCE_SHUFFLE,
            SLEEP, YAWN, THINK, READ, PLAY, STRETCH, SIT, WAVE, PEEK, LOOK_LEFT, LOOK_RIGHT,
        )

        fun toyAutonomy(cosmetics: PetCosmetics): List<PetAnim> = buildList {
            if (cosmetics.ownsBall) add(PLAY_BALL)
            if (cosmetics.ownsChair) add(CHAIR_ROCK)
        }
    }
}
