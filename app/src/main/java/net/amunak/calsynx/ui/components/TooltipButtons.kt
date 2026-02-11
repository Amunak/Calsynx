package net.amunak.calsynx.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
	tooltip: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	content: @Composable () -> Unit
) {
	TooltipBox(
		positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
		tooltip = { PlainTooltip { androidx.compose.material3.Text(text = tooltip) } },
		state = rememberTooltipState()
	) {
		IconButton(
			onClick = onClick,
			enabled = enabled,
			modifier = modifier
		) {
			content()
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipFloatingActionButton(
	tooltip: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	TooltipBox(
		positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
		tooltip = { PlainTooltip { androidx.compose.material3.Text(text = tooltip) } },
		state = rememberTooltipState()
	) {
		FloatingActionButton(
			onClick = onClick,
			modifier = modifier
		) {
			content()
		}
	}
}
