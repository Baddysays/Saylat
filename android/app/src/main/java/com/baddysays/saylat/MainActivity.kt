package com.baddysays.saylat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baddysays.saylat.ui.BrowserScreen
import com.baddysays.saylat.ui.BrowserViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.baddysays.saylat.ui.theme.AppThemeId
import com.baddysays.saylat.ui.theme.SaylatTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_START_URL = "extra_start_url"
    }

    private var pendingStartUrl: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SaylatApp
        pendingStartUrl = intent?.getStringExtra(EXTRA_START_URL)
        setContent {
            val vm: BrowserViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return BrowserViewModel(app.prefs, applicationContext) as T
                        }
                    },
            )
            LaunchedEffect(pendingStartUrl) {
                pendingStartUrl?.let { url ->
                    vm.openExternalUrl(url)
                    pendingStartUrl = null
                }
            }
            val appTheme by vm.state.map { it.appTheme }.collectAsState(initial = AppThemeId.TEAL)
            SaylatTheme(themeId = appTheme) {
                BrowserScreen(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingStartUrl = intent.getStringExtra(EXTRA_START_URL)
    }
}
