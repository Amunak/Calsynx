package net.amunak.calsynx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ScrollIndicator(
	state: LazyListState,
	modifier: Modifier = Modifier,
	minThumbHeight: Dp = 24.dp
) {
	val layoutInfo = state.layoutInfo
	val visibleItems = layoutInfo.visibleItemsInfo
	if (visibleItems.isEmpty()) return
	if (!state.canScrollBackward && !state.canScrollForward) return
	val totalItems = layoutInfo.totalItemsCount
	if (totalItems <= 0) return
	val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
	val averageSpacing = if (visibleItems.size >= 2) {
		val spacings = visibleItems.zipWithNext { current, next ->
			(next.offset - current.offset - current.size).coerceAtLeast(0)
		}
		spacings.average().toFloat()
	} else {
		0f
	}
	val totalContentHeight = averageItemSize * totalItems +
		averageSpacing * (totalItems - 1) +
		layoutInfo.beforeContentPadding +
		layoutInfo.afterContentPadding
	if (totalContentHeight <= 0f) return

	val alpha = if (state.isScrollInProgress) 0.85f else 0.35f
	val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
	val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
	val minThumbPx = with(LocalDensity.current) { minThumbHeight.toPx() }

	Canvas(
		modifier = modifier
			.fillMaxHeight()
			.width(5.dp)
			.padding(vertical = 4.dp)
	) {
		val viewportHeight = size.height
		val effectiveContentHeight = max(totalContentHeight, viewportHeight + 1f)

		val scrollOffset =
			state.firstVisibleItemIndex * (averageItemSize + averageSpacing) +
				state.firstVisibleItemScrollOffset
		val rawThumbHeight = (viewportHeight / effectiveContentHeight) * viewportHeight
		val thumbHeight = rawThumbHeight.coerceAtLeast(minThumbPx)
		val maxOffset = (viewportHeight - thumbHeight).coerceAtLeast(0f)
		val thumbOffset = ((scrollOffset / effectiveContentHeight) * viewportHeight)
			.coerceIn(0f, maxOffset)
		val radius = size.width / 2f

		drawRoundRect(
			color = trackColor,
			topLeft = Offset(x = 0f, y = 0f),
			size = Size(width = size.width, height = viewportHeight),
			cornerRadius = CornerRadius(radius, radius)
		)
		drawRoundRect(
			color = thumbColor,
			topLeft = Offset(x = 0f, y = thumbOffset),
			size = Size(width = size.width, height = thumbHeight),
			cornerRadius = CornerRadius(radius, radius)
		)
	}
}
@Preview(showBackground = true)
@Composable
private fun ScrollIndicatorPreview() {
	val state = rememberLazyListState()
	Surface {
		Box(modifier = Modifier.fillMaxSize()) {
			LazyColumn(
				state = state,
				modifier = Modifier.fillMaxSize()
			) {
				items(40) { index ->
					Text(
						text = "Item $index",
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
					)
				}
			}
			ScrollIndicator(
				state = state,
				modifier = Modifier.align(Alignment.CenterEnd)
			)
		}
	}
}
