package com.baddysays.saylat.prefs

enum class PetShopCategory { HATS, TOYS }

data class PetShopItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: PetShopCategory,
    /** Цена в байтах кошелька (отображаем как КБ/МБ). */
    val priceBytes: Long,
    val isHat: Boolean = false,
    val isToy: Boolean = false,
)

object PetShopCatalog {
    const val HAT_LEAVES = "hat_leaves"

    const val TOY_BALL = "toy_ball"
    const val TOY_CHAIR = "toy_chair"

    /** Базовая цена в KiB × 100 (экономия трафика). */
    private fun priceKb(kib: Long): Long = kib * 1024L * 100L

    val all: List<PetShopItem> = listOf(
        PetShopItem(
            id = HAT_LEAVES,
            title = "Листочки",
            subtitle = "Родная корона Saylat",
            category = PetShopCategory.HATS,
            priceBytes = 0L,
            isHat = true,
        ),
        PetShopItem(
            id = "hat_beanie",
            title = "Бини",
            subtitle = "Уютная шапка",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(45),
            isHat = true,
        ),
        PetShopItem(
            id = "hat_visor",
            title = "Козырёк",
            subtitle = "Спортивный стиль",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(55),
            isHat = true,
        ),
        PetShopItem(
            id = "hat_party",
            title = "Праздник",
            subtitle = "Конфетти в пикселях",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(75),
            isHat = true,
        ),
        PetShopItem(
            id = "hat_sombrero",
            title = "Сомбреро",
            subtitle = "Широкие поля",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(95),
            isHat = true,
        ),
        PetShopItem(
            id = "hat_cowboy",
            title = "Ковбой",
            subtitle = "Дикий Запад",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(110),
            isHat = true,
        ),
        PetShopItem(
            id = "hat_crown",
            title = "Корона",
            subtitle = "Король салата",
            category = PetShopCategory.HATS,
            priceBytes = priceKb(140),
            isHat = true,
        ),
        PetShopItem(
            id = TOY_BALL,
            title = "Мячик",
            subtitle = "Игра и прыжки",
            category = PetShopCategory.TOYS,
            priceBytes = priceKb(85),
            isToy = true,
        ),
        PetShopItem(
            id = TOY_CHAIR,
            title = "Качалка",
            subtitle = "Кресло-качалка",
            category = PetShopCategory.TOYS,
            priceBytes = priceKb(125),
            isToy = true,
        ),
    )

    fun find(id: String): PetShopItem? = all.find { it.id == id }

    val defaultOwned: Set<String> = setOf(HAT_LEAVES)
}
