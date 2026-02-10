package net.amunak.calscium.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ToastMessage(
	message: String?,
	onShown: () -> Unit
) {
	val context = LocalContext.current
	LaunchedEffect(message) {
		if (message.isNullOrBlank()) return@LaunchedEffect
		Toast.makeText(context, message, Toast.LENGTH_LONG).show()
		onShown()
	}
}

@Preview(showBackground = true)
@Composable
private fun ToastMessagePreview() {
	ToastMessage(message = "Saved", onShown = {})
}
