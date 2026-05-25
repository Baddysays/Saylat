# Умная вёрстка (прототип)

## Поток

1. Сервер отдаёт `SaylatArticle` с блоками (`heading`, `paragraph`, `image`, …).
2. **Фаза 1** — `HeuristicLayoutEnhancer` → `LayoutPlan` → мгновенный `RenderPlan`.
3. **Фаза 2 (опция)** — `PrototypeAiLayoutEnhancer` (~700 ms) уточняет план:
   - скрывает мусорные короткие абзацы;
   - склеивает соседние короткие параграфы;
   - переносит картинки в конец, лимит по `density`;
   - улучшает `hero_excerpt`.
4. `LayoutPlanRenderer` рисует карточки Compose.

## Настройки

- Переключатель в приложении: **Умная вёрстка (локально)**.
- Доступно при RAM устройства **≥ 3500 МБ** (`DeviceCapabilities`).
- По умолчанию **выкл**.

## Контракт плана

[`shared/layout-plan.schema.json`](../shared/layout-plan.schema.json)

## Следующий шаг

Заменить `PrototypeAiLayoutEnhancer` на [`GemmaLayoutEnhancer`](../android/app/src/main/java/com/baddysays/saylat/ai/GemmaLayoutEnhancer.kt) с LiteRT-LM: промпт = JSON блоков → ответ = `LayoutPlan`.
