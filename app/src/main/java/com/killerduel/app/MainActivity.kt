package com.killerduel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.killerduel.app.data.DataStoreGameRepository
import com.killerduel.app.game.GameViewModel
import com.killerduel.app.ui.KillerDuelApp
import com.killerduel.app.ui.theme.KillerDuelTheme
import com.killerduel.app.ui.theme.Palette
import com.killerduel.app.ui.theme.paletteFor

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DataStoreGameRepository(applicationContext)
        viewModel = ViewModelProvider(this, GameViewModel.factory(repository))[GameViewModel::class.java]

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val theme = state.settings.theme

            // Les icônes des barres système doivent virer au clair sur fond sombre.
            val view = LocalView.current
            SideEffect {
                val light = !paletteFor(theme).isDark
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }

            KillerDuelTheme(theme) {
                // La surface couvre l'écran entier, barres système comprises ;
                // seul le contenu est décalé, sinon un bandeau clair subsiste.
                Surface(modifier = Modifier.fillMaxSize(), color = Palette.Background) {
                    Box(Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
                        KillerDuelApp(viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        // L'utilisateur peut quitter à tout moment : le chronomètre ne doit pas
        // continuer à courir pour lui, et la grille en cours est conservée.
        viewModel.onAppBackgrounded()
        super.onStop()
    }
}
