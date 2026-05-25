package com.baddysays.saylat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baddysays.saylat.ui.BrowserScreen
import com.baddysays.saylat.ui.BrowserViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.baddysays.saylat.ui.theme.AppThemeId
import com.baddysays.saylat.ui.theme.SaylatTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SaylatApp
        setContent {
            val vm: BrowserViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return BrowserViewModel(app.prefs, applicationContext) as T
                        }
                    },
            )
            val appTheme by vm.state.map { it.appTheme }.collectAsState(initial = AppThemeId.TEAL)
            SaylatTheme(themeId = appTheme) {
                BrowserScreen(vm)
            }
        }
    }
}
