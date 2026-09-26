package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.TabType
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TabBar(
    selectedTab: TabType,
    tabOrder: List<TabType>,
    onTabSelected: (TabType) -> Unit,
    onReorderTabs: (fromIndex: Int, toIndex: Int) -> Unit,
    isTopPlaced: Boolean,
    modifier: Modifier = Modifier,
    dynamicAccentColor: Color = Color.White,
    pagerOffset: Float = 0f
) {
    val tabMetaMap = remember {
        mapOf(
            TabType.PLAYLISTS to TabItem(TabType.PLAYLISTS, "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
            TabType.SONGS to TabItem(TabType.SONGS, "Songs", Icons.Default.MusicNote),
            TabType.FOLDERS to TabItem(TabType.FOLDERS, "Folders", Icons.Default.Folder),
            TabType.ARTISTS to TabItem(TabType.ARTISTS, "Artists", Icons.Default.Person),
            TabType.SEARCH to TabItem(TabType.SEARCH, "Search", Icons.Default.Search),
            TabType.FAVORITES to TabItem(TabType.FAVORITES, "Favorites", Icons.Default.Favorite),
            TabType.DUSTY_TRACKS to TabItem(TabType.DUSTY_TRACKS, "Dusty", Icons.Default.Archive)
        )
    }

    val paddingModifier = if (isTopPlaced) {
        modifier.statusBarsPadding()
    } else {
        modifier.navigationBarsPadding()
    }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = paddingModifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .glassmorphic(shape = RoundedCornerShape(22.dp), backgroundColor = DarkCardGlass)
            .testTag("app_tab_bar")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            val totalWidth = maxWidth
            val count = tabOrder.size.coerceAtLeast(1)
            val tabWidth = totalWidth / count
            val indicatorShape = RoundedCornerShape(16.dp)

            // Real-time gesture-following indicator pill that glides with finger swipe
            val clampedOffset = pagerOffset.coerceIn(0f, (count - 1).toFloat())
            val indicatorX = tabWidth * clampedOffset

            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .width(tabWidth)
                    .height(52.dp)
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .clip(indicatorShape)
                    .background(dynamicAccentColor, indicatorShape)
            )

            // Evenly distributed tabs across the full bar width
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabOrder.forEachIndexed { index, tabType ->
                    val item = tabMetaMap[tabType] ?: TabItem(tabType, tabType.label, Icons.Default.MusicNote)
                    val isDragging = (draggingIndex == index)
                    val diff = abs(clampedOffset - index)
                    val selectProgress = (1f - diff).coerceIn(0f, 1f)

                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isDragging) 1.15f else 1.0f,
                        label = "tab_scale"
                    )

                    // Interpolate content color from muted to black as indicator slides under it
                    val contentColor = lerp(TextMuted, Color.Black, selectProgress)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .zIndex(if (isDragging) 5f else 1f)
                            .scale(scaleAnim)
                            .offset {
                                if (isDragging) IntOffset(dragOffsetX.roundToInt(), 0) else IntOffset.Zero
                            }
                            .clip(indicatorShape)
                            .pointerInput(tabType, index, count) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        val threshold = with(this) { tabWidth.toPx() * 0.8f }
                                        if (dragOffsetX > threshold && draggingIndex in 0 until tabOrder.lastIndex) {
                                            val target = draggingIndex + 1
                                            onReorderTabs(draggingIndex, target)
                                            draggingIndex = target
                                            dragOffsetX -= threshold
                                        } else if (dragOffsetX < -threshold && draggingIndex > 0) {
                                            val target = draggingIndex - 1
                                            onReorderTabs(draggingIndex, target)
                                            draggingIndex = target
                                            dragOffsetX += threshold
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = -1
                                        dragOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffsetX = 0f
                                    }
                                )
                            }
                            .pointerInput(tabType) {
                                detectTapGestures(
                                    onTap = { onTabSelected(tabType) }
                                )
                            }
                            .padding(vertical = 4.dp)
                            .testTag("tab_${tabType.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        val isActive = (selectProgress > 0.45f)
                        if (isActive) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = item.label,
                                    color = contentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TabItem(
    val type: TabType,
    val label: String,
    val icon: ImageVector
)
