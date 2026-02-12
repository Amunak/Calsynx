package net.amunak.calsynx.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScreenSurface(
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	Surface(
		modifier = modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.surfaceContainerLowest
	) {
		content()
	}
}
