package net.amunak.calscium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun CalendarLabel(
	name: String,
	color: Int?,
	modifier: Modifier = Modifier,
	textColor: Color = MaterialTheme.colorScheme.onSurface,
	textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically
	) {
		if (color != null) {
			Box(
				modifier = Modifier
					.size(10.dp)
					.background(Color(color), CircleShape)
			)
			Spacer(modifier = Modifier.size(6.dp))
		}
		Text(
			text = name,
			color = textColor,
			style = textStyle,
			maxLines = 1
		)
	}
}
