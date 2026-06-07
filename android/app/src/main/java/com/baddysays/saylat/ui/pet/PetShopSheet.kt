package com.baddysays.saylat.ui.pet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baddysays.saylat.prefs.PetProfile
import com.baddysays.saylat.prefs.PetShopCatalog
import com.baddysays.saylat.prefs.PetShopCategory
import com.baddysays.saylat.prefs.PetShopItem
import com.baddysays.saylat.prefs.PetWallet
import com.baddysays.saylat.ui.PetMood
import com.baddysays.saylat.ui.PixelPetPreview

private val pixelBorder = Color(0xFF1E272E)
private val pixelMint = Color(0xFF55C57A)
private val pixelMintDark = Color(0xFF2D6A4F)
private val pixelGold = Color(0xFFF9CA24)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PetShopSheet(
    visible: Boolean,
    profile: PetProfile,
    shopMessage: String?,
    onDismiss: () -> Unit,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
    onUnequipToys: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(pixelBorder.copy(alpha = 0.25f)),
                )
            }
        },
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            ShopHeader(profile = profile, shopMessage = shopMessage)
            ShopCategorySection(
                title = "Шляпы",
                items = PetShopCatalog.all.filter { it.category == PetShopCategory.HATS },
                profile = profile,
                onBuy = onBuy,
                onEquip = onEquip,
            )
            ShopCategorySection(
                title = "Игрушки",
                items = PetShopCatalog.all.filter { it.category == PetShopCategory.TOYS },
                profile = profile,
                onBuy = onBuy,
                onEquip = onEquip,
            )
            if (profile.ballEquipped || profile.chairEquipped) {
                FilledTonalButton(
                    onClick = onUnequipToys,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Убрать игрушку", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(profile: PetProfile, shopMessage: String?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = pixelMintDark.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(2.dp, pixelMint.copy(alpha = 0.45f)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PixelPetPreview(
                stage = profile.stage.coerceAtLeast(2),
                mood = PetMood.HAPPY,
                anim = if (profile.chairEquipped) PetAnim.CHAIR_ROCK else if (profile.ballEquipped) PetAnim.PLAY_BALL else PetAnim.IDLE_BREATHE,
                frame = 0,
                cosmetics = PetCosmetics.from(profile),
                frameVariant = HedgehogFrameVariant.Stage,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pixelMint.copy(alpha = 0.15f))
                    .border(2.dp, pixelBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Магазин ${profile.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pixelGold.copy(alpha = 0.35f))
                            .border(2.dp, pixelBorder.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "💾 ${PetWallet.formatWallet(profile)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
                Text(
                    "Кошелёк пополняется за экономию трафика Saylat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                shopMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopCategorySection(
    title: String,
    items: List<PetShopItem>,
    profile: PetProfile,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        color = pixelMintDark,
    )
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { item ->
                    ShopItemCard(
                        item = item,
                        profile = profile,
                        onBuy = onBuy,
                        onEquip = onEquip,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: PetShopItem,
    profile: PetProfile,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val owned = profile.owns(item.id)
    val equipped = when {
        item.isHat -> profile.equippedHatId == item.id
        item.id == PetShopCatalog.TOY_BALL -> profile.ballEquipped
        item.id == PetShopCatalog.TOY_CHAIR -> profile.chairEquipped
        else -> false
    }
    val canAfford = item.priceBytes <= 0 || profile.walletBytes >= item.priceBytes
    val transition = rememberInfiniteTransition(label = "shop")
    val previewPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "pf",
    )
    val previewFrame = previewPhase.toInt()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (equipped) pixelMint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (equipped) pixelMint else pixelBorder.copy(alpha = 0.18f),
        ),
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (item.isHat) {
                PixelPetPreview(
                    stage = 2,
                    mood = PetMood.HAPPY,
                    anim = PetAnim.IDLE,
                    frame = previewFrame,
                    cosmetics = PetCosmetics(hatId = item.id),
                    frameVariant = HedgehogFrameVariant.Mini,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Canvas(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF8EE))
                        .border(1.dp, pixelBorder.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                ) {
                    with(PetHedgehogEngine) {
                        drawShopPreview(item.id, previewFrame)
                    }
                }
            }
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                if (item.priceBytes <= 0) "Бесплатно" else PetWallet.formatPrice(item.priceBytes),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (canAfford || owned) pixelMintDark else MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
            when {
                equipped -> Text("Надето ✓", fontFamily = FontFamily.Monospace, color = pixelMintDark, fontSize = 11.sp)
                owned && item.isHat -> Button(
                    onClick = { onEquip(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) { Text("Надеть", fontSize = 11.sp) }
                owned -> Button(
                    onClick = { onEquip(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) { Text("Играть", fontSize = 11.sp) }
                item.priceBytes <= 0 -> Button(
                    onClick = { onEquip(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) { Text("Выбрать", fontSize = 11.sp) }
                else -> Button(
                    onClick = { onBuy(item.id) },
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pixelMintDark),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Text("Купить", fontSize = 11.sp)
                }
            }
        }
    }
}
