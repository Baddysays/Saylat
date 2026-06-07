package com.baddysays.saylat.ui



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.PushPin

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.filled.StarBorder

import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.Share

import androidx.compose.material.icons.filled.SaveAlt

import androidx.compose.material.icons.filled.BookmarkBorder

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Translate

import androidx.compose.material.icons.filled.RecordVoiceOver

import androidx.compose.material.icons.filled.FindInPage

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.IconButtonDefaults

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.strings.SaylatStrings
import com.baddysays.saylat.prefs.PetProfile

@Composable

fun SaylatTopBar(

    screen: AppScreen,

    activeQuery: String?,

    networkOnline: Boolean = true,

    onBack: () -> Unit,

    onHome: () -> Unit,

    onSettings: () -> Unit,

    onSearchFocus: (() -> Unit)? = null,

    tamagotchiEnabled: Boolean = true,

    petProfile: PetProfile = PetProfile(),

    uiLanguage: AppLanguage = AppLanguage.RU,

    showTranslate: Boolean = false,

    translationActive: Boolean = false,

    translating: Boolean = false,

    onToggleTranslate: () -> Unit = {},

    showGallerySave: Boolean = false,

    gallerySaveInProgress: Boolean = false,

    onSaveGallery: () -> Unit = {},

    showFavoriteActions: Boolean = false,

    isFavorite: Boolean = false,

    onToggleFavorite: () -> Unit = {},

    onPinShortcut: () -> Unit = {},

    showShare: Boolean = false,

    onShare: () -> Unit = {},

    readLaterCount: Int = 0,

    onOpenReadLater: (() -> Unit)? = null,

    showReadLaterAction: Boolean = false,

    isReadLater: Boolean = false,

    onToggleReadLater: () -> Unit = {},

    showToc: Boolean = false,

    onTocClick: () -> Unit = {},

    showArticleSearch: Boolean = false,

    articleSearchActive: Boolean = false,

    onArticleSearchClick: () -> Unit = {},

    showTts: Boolean = false,

    onTtsClick: () -> Unit = {},

) {

    val barColor = MaterialTheme.colorScheme.background

    val (title, subtitle) = when (screen) {

        AppScreen.HOME -> null to null

        AppScreen.SEARCH_RESULTS -> (activeQuery?.let { " «$it»" } ?: "Поиск") to "результаты"

        AppScreen.READER -> "Чтение" to "сжатая лента"

        AppScreen.FEED -> "Лента" to "Пикабу · ВК · Дзен"

    }



    Column(

        modifier = Modifier

            .fillMaxWidth()

            .statusBarsPadding(),

    ) {

        Surface(

            modifier = Modifier.fillMaxWidth(),

            color = barColor,

            tonalElevation = 0.dp,

            shadowElevation = 0.dp,

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 6.dp, vertical = 4.dp),

                verticalAlignment = Alignment.CenterVertically,

            ) {

                if (screen != AppScreen.HOME) {

                    IconButton(

                        onClick = onBack,

                        modifier = Modifier.padding(0.dp),

                    ) {

                        Icon(

                            Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription = "Назад",

                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),

                        )

                    }

                }



                when (screen) {

                    AppScreen.HOME -> HeaderPetSlot(

                        tamagotchiEnabled = tamagotchiEnabled,

                        profile = petProfile,

                        uiLanguage = uiLanguage,

                        modifier = Modifier.weight(1f),

                    )

                    else -> {

                        val t = title ?: ""

                        val s = subtitle ?: ""

                        Column(

                            modifier = Modifier

                                .weight(1f)

                                .padding(horizontal = 8.dp),

                        ) {

                            Text(

                                t,

                                style = MaterialTheme.typography.titleMedium,

                                fontWeight = FontWeight.Bold,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis,

                            )

                            Text(

                                s,

                                style = MaterialTheme.typography.labelSmall,

                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis,

                            )

                        }

                    }

                }



                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {

                    if (screen == AppScreen.HOME && onSearchFocus != null) {

                        IconButton(onClick = onSearchFocus) {

                            Icon(

                                Icons.Default.Search,

                                contentDescription = "Поиск",

                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),

                            )

                        }

                    }

                    if (screen == AppScreen.HOME && onOpenReadLater != null) {

                        IconButton(onClick = onOpenReadLater) {

                            Icon(

                                Icons.Default.BookmarkBorder,

                                contentDescription = "Прочитать позже",

                                tint = if (readLaterCount > 0) {

                                    MaterialTheme.colorScheme.primary

                                } else {

                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

                                },

                            )

                        }

                    }

                    if (showReadLaterAction) {

                        ReadLaterTopBarButton(isSaved = isReadLater, onToggle = onToggleReadLater)

                    }

                    if (showToc) {

                        TocButton(onClick = onTocClick)

                    }

                    if (showArticleSearch) {

                        IconButton(onClick = onArticleSearchClick) {

                            Icon(

                                Icons.Default.FindInPage,

                                contentDescription = "Поиск в статье",

                                tint = if (articleSearchActive) {

                                    MaterialTheme.colorScheme.primary

                                } else {

                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

                                },

                            )

                        }

                    }

                    if (showTts) {

                        IconButton(onClick = onTtsClick) {

                            Icon(

                                Icons.Default.RecordVoiceOver,

                                contentDescription = "Озвучить",

                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),

                            )

                        }

                    }

                    if (showFavoriteActions) {

                        IconButton(onClick = onToggleFavorite) {

                            Icon(

                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,

                                contentDescription = "В избранное",

                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),

                            )

                        }

                        IconButton(onClick = onPinShortcut) {

                            Icon(

                                Icons.Default.PushPin,

                                contentDescription = "На рабочий стол",

                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),

                            )

                        }

                    }

                    if (showShare) {

                        IconButton(onClick = onShare) {

                            Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))

                        }

                    }

                    if (showGallerySave) {

                        IconButton(onClick = onSaveGallery, enabled = !gallerySaveInProgress) {

                            Icon(Icons.Default.SaveAlt, contentDescription = "Сохранить в галерею", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))

                        }

                    }

                    if (showTranslate) {

                        IconButton(onClick = onToggleTranslate, enabled = !translating) {

                            Icon(

                                Icons.Default.Translate,

                                contentDescription = "Перевести",

                                tint = if (translationActive) {

                                    MaterialTheme.colorScheme.primary

                                } else {

                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

                                },

                            )

                        }

                    }

                    if (screen != AppScreen.HOME) {

                        IconButton(onClick = onHome) {

                            Icon(Icons.Default.Home, contentDescription = "Домой", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))

                        }

                    }

                    IconButton(onClick = onSettings) {

                        Icon(

                            Icons.Default.MoreVert,

                            contentDescription = "Меню",

                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),

                        )

                    }

                }

            }

        }

    }

}

