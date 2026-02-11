package net.amunak.calsynx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun WarningCard(
	title: String,
	message: String,
	actions: @Composable RowScope.() -> Unit
) {
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = Icons.Default.Warning,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error,
					modifier = Modifier.size(18.dp)
				)
				Text(
					text = title,
					style = MaterialTheme.typography.titleSmall,
					modifier = Modifier.padding(start = 6.dp)
				)
			}
			Text(
				text = message,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				content = actions
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun WarningCardPreview() {
	WarningCard(
		title = "Background sync",
		message = "Allow background work for reliable updates."
	) {
		Text(text = "Action")
		Spacer(modifier = Modifier.size(8.dp))
		Text(text = "Secondary")
	}
}
