package com.karishma.swiggyanimation.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MusicPlayerScreen(onBack: () -> Unit) {
    val viewModel: MusicPlayerViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        MusicPlayerState.Loading -> MusicLoadingScreen()
        is MusicPlayerState.Error -> MusicErrorScreen(message = s.message, onBack = onBack)
        is MusicPlayerState.Ready -> {
            AnimatedContent(
                targetState = s.showLibrary,
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically(tween(350)) { it } + fadeIn(tween(250))) togetherWith
                            fadeOut(tween(200))
                    } else {
                        (fadeIn(tween(250))) togetherWith
                            (slideOutVertically(tween(350)) { it } + fadeOut(tween(200)))
                    }
                },
                label = "screenSwitch",
            ) { showLibrary ->
                if (showLibrary) {
                    LibraryScreen(state = s, onIntent = viewModel::onIntent)
                } else {
                    PlayerContent(state = s, onIntent = viewModel::onIntent)
                }
            }
        }
    }
}
