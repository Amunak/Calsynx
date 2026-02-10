package net.amunak.calscium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.amunak.calscium.ui.theme.CalsciumTheme
import net.amunak.calscium.ui.CalsciumAppRoute

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			CalsciumTheme {
				CalsciumAppRoute()
			}
		}
	}
}
