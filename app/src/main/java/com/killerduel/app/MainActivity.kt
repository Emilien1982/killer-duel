package com.killerduel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.killerduel.app.data.DataStoreGameRepository
import com.killerduel.app.game.GameViewModel
import com.killerduel.app.ui.KillerDuelApp
import com.killerduel.app.ui.theme.KillerDuelTheme
import com.killerduel.app.ui.theme.Palette

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DataStoreGameRepository(applicationContext)
        viewModel = ViewModelProvider(this, GameViewModel.factory(repository))[GameViewModel::class.java]

        setContent {
            KillerDuelTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = Palette.Background
                ) {
                    KillerDuelApp(viewModel)
                }
            }
        }
    }

    override fun onStop() {
        // L'utilisateur peut quitter à tout moment : la grille en cours est conservée.
        viewModel.saveProgress()
        super.onStop()
    }
}
