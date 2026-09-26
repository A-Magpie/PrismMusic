package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FloatingScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentCyan
) {
    val totalCount = listState.layoutInfo.totalItemsCount
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val visibleCount = visibleItems.size

    if (totalCount <= 0 || visibleCount >= totalCount) return

    var isDragging by remember { mutableStateOf(false) }
    var targetAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(listState.isScrollInProgress, isDragging) {
        if (listState.isScrollInProgress || isDragging) {
            targetAlpha = 1f
        } else {
            delay(1200L)
            targetAlpha = 0f
        }
    }

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 350),
        label = "list_scrollbar_alpha"
    )

    if (alpha <= 0.01f && !isDragging) return

    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .padding(end = 3.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val containerHeight = maxHeight
        val heightRatio = (visibleCount.toFloat() / totalCount.toFloat()).coerceIn(0.08f, 0.45f)
        val thumbHeight = containerHeight * heightRatio
        val travelDistance = containerHeight - thumbHeight

        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        val firstSize = visibleItems.firstOrNull()?.size?.toFloat() ?: 1f
        val fractionalIndex = firstIndex + (firstOffset / firstSize.coerceAtLeast(1f))
        val maxIndex = (totalCount - visibleCount).toFloat().coerceAtLeast(1f)
        val scrollFraction = (fractionalIndex / maxIndex).coerceIn(0f, 1f)
        val thumbOffset = travelDistance * scrollFraction

        Box(
            modifier = Modifier
                .offset(y = thumbOffset)
                .align(Alignment.TopEnd)
                .size(width = 4.dp, height = thumbHeight)
                .background(accentColor, shape = CircleShape)
                .pointerInput(totalCount) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val travelPx = travelDistance.toPx()
                            if (travelPx > 0f) {
                                val currentY = (travelPx * scrollFraction) + dragAmount
                                val newFraction = (currentY / travelPx).coerceIn(0f, 1f)
                                val targetItem = (newFraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetItem)
                                }
                            }
                        }
                    )
                }
        )
    }
}

@Composable
fun FloatingScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentCyan
) {
    val totalCount = gridState.layoutInfo.totalItemsCount
    val visibleItems = gridState.layoutInfo.visibleItemsInfo
    val visibleCount = visibleItems.size

    if (totalCount <= 0 || visibleCount >= totalCount) return

    var isDragging by remember { mutableStateOf(false) }
    var targetAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            targetAlpha = 1f
        } else {
            delay(1200L)
            targetAlpha = 0f
        }
    }

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 350),
        label = "grid_scrollbar_alpha"
    )

    if (alpha <= 0.01f && !isDragging) return

    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .padding(end = 3.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val containerHeight = maxHeight
        val heightRatio = (visibleCount.toFloat() / totalCount.toFloat()).coerceIn(0.08f, 0.45f)
        val thumbHeight = containerHeight * heightRatio
        val travelDistance = containerHeight - thumbHeight

        val firstIndex = gridState.firstVisibleItemIndex
        val firstOffset = gridState.firstVisibleItemScrollOffset
        val firstSize = visibleItems.firstOrNull()?.size?.height?.toFloat() ?: 1f
        val fractionalIndex = firstIndex + (firstOffset / firstSize.coerceAtLeast(1f))
        val maxIndex = (totalCount - visibleCount).toFloat().coerceAtLeast(1f)
        val scrollFraction = (fractionalIndex / maxIndex).coerceIn(0f, 1f)
        val thumbOffset = travelDistance * scrollFraction

        Box(
            modifier = Modifier
                .offset(y = thumbOffset)
                .align(Alignment.TopEnd)
                .size(width = 4.dp, height = thumbHeight)
                .background(accentColor, shape = CircleShape)
                .pointerInput(totalCount) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val travelPx = travelDistance.toPx()
                            if (travelPx > 0f) {
                                val currentY = (travelPx * scrollFraction) + dragAmount
                                val newFraction = (currentY / travelPx).coerceIn(0f, 1f)
                                val targetItem = (newFraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                                coroutineScope.launch {
                                    gridState.scrollToItem(targetItem)
                                }
                            }
                        }
                    )
                }
        )
    }
}
