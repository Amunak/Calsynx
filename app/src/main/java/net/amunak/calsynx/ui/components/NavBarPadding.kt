package net.amunak.calsynx.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

data class NavBarPadding(
	val bottom: Dp,
	val start: Dp,
	val end: Dp
)

@Composable
fun rememberNavBarPadding(): NavBarPadding {
	val density = LocalDensity.current
	val layoutDirection = LocalLayoutDirection.current
	val insets = WindowInsets.navigationBars
	return NavBarPadding(
		bottom = with(density) { insets.getBottom(this).toDp() },
		start = with(density) { insets.getLeft(this, layoutDirection).toDp() },
		end = with(density) { insets.getRight(this, layoutDirection).toDp() }
	)
}
