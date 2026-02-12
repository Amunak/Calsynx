package net.amunak.calsynx.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.ui.components.ScreenSurface
import net.amunak.calsynx.ui.components.rememberNavBarPadding
import net.amunak.calsynx.R
import net.amunak.calsynx.ui.components.ScrollIndicator
import net.amunak.calsynx.ui.components.TooltipIconButton
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
	val navBar = rememberNavBarPadding()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.title_sync_logs)) },
				navigationIcon = {
					TooltipIconButton(
						tooltip = stringResource(R.string.action_back),
						onClick = onBack
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				},
				actions = {
					TooltipIconButton(
						tooltip = stringResource(R.string.action_refresh),
						onClick = onRefresh
					) {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = stringResource(R.string.action_refresh)
						)
					}
					TooltipIconButton(
						tooltip = stringResource(R.string.action_share),
						onClick = onShareLogs
					) {
						Icon(
							imageVector = Icons.Default.Share,
							contentDescription = stringResource(R.string.action_share)
						)
					}
					TooltipIconButton(
						tooltip = stringResource(R.string.action_clear_logs),
						onClick = onClearLogs
					) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = stringResource(R.string.action_clear_logs)
						)
					}
				}
			)
		}
	) { padding ->
		ScreenSurface {
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
				val listState = rememberLazyListState()
				Box(modifier = Modifier.fillMaxSize()) {
					LazyColumn(
						state = listState,
						modifier = Modifier
							.fillMaxSize()
							.padding(padding),
						contentPadding = PaddingValues(
							start = 16.dp,
							end = 16.dp,
							top = 16.dp,
							bottom = 16.dp + navBar.bottom
						),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						items(state.lines.asReversed()) { line ->
							Text(
								text = line,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.fillMaxWidth()
							)
						}
						item { Spacer(modifier = Modifier.height(8.dp)) }
					}
					ScrollIndicator(
						state = listState,
						modifier = Modifier
							.align(Alignment.CenterEnd)
							.padding(top = padding.calculateTopPadding())
							.padding(bottom = navBar.bottom)
							.padding(end = navBar.end + 2.dp)
					)
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
