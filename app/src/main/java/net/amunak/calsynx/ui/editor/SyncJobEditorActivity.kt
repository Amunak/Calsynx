package net.amunak.calsynx.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.amunak.calsynx.ui.theme.CalsynxTheme

class SyncJobEditorActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			CalsynxTheme {
				SyncJobEditorRoute(
					jobId = intent.getLongExtra(EXTRA_JOB_ID, INVALID_JOB_ID)
						.takeIf { it != INVALID_JOB_ID },
					onClose = { finish() }
				)
			}
		}
	}

	companion object {
		private const val EXTRA_JOB_ID = "job_id"
		private const val INVALID_JOB_ID = -1L

		fun newIntent(context: Context, jobId: Long? = null): Intent {
			return Intent(context, SyncJobEditorActivity::class.java).apply {
				if (jobId != null) {
					putExtra(EXTRA_JOB_ID, jobId)
				}
			}
		}
	}
}

@Composable
private fun SyncJobEditorRoute(
	jobId: Long?,
	onClose: () -> Unit
) {
	val viewModel: SyncJobEditorViewModel = viewModel()
	val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
	LaunchedEffect(jobId) {
		viewModel.load(jobId)
	}
	LaunchedEffect(uiState.saveState) {
		if (uiState.saveState == SaveState.Success) {
			onClose()
		}
	}

	SyncJobEditorScreen(
		state = uiState,
		onClose = onClose,
		onSave = viewModel::save
	)
}
