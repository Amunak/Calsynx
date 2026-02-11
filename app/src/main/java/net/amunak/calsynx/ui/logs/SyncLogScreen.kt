package net.amunak.calsynx.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.R
import net.amunak.calsynx.ui.theme.CalsynxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLogScreen(
	state: SyncLogUiState,
	onBack: () -> Unit,
	onClearLogs: () -> Unit,
	onShareLogs: () -> Unit,
	onRefresh: () -> Unit
) {
	LaunchedEffect(Unit) {
		onRefresh()
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.title_sync_logs)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				},
				actions = {
					IconButton(onClick = onRefresh) {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = stringResource(R.string.action_refresh)
						)
					}
					IconButton(onClick = onShareLogs) {
						Icon(
							imageVector = Icons.Default.Share,
							contentDescription = stringResource(R.string.action_share)
						)
					}
					IconButton(onClick = onClearLogs) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = stringResource(R.string.action_clear_logs)
						)
					}
				}
			)
		}
	) { padding ->
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.surfaceContainerLowest
		) {
			if (state.lines.isEmpty() && !state.isLoading) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding)
						.padding(16.dp),
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = stringResource(R.string.label_no_logs),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			} else {
				LazyColumn(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					contentPadding = PaddingValues(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					items(state.lines) { line ->
						Text(
							text = line,
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.fillMaxWidth()
						)
					}
					item { Spacer(modifier = Modifier.height(8.dp)) }
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun SyncLogScreenPreview() {
	CalsynxTheme {
		SyncLogScreen(
			state = SyncLogUiState(
				lines = listOf("2026-02-11 10:00 Sync completed", "2026-02-11 10:05 Sync failed")
			),
			onBack = {},
			onClearLogs = {},
			onShareLogs = {},
			onRefresh = {}
		)
	}
}
