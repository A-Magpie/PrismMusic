package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.SortBy
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode

@Composable
fun SortHeader(
    itemCount: Int,
    sortBy: SortBy,
    sortDirection: SortDirection,
    onSortByChanged: (SortBy) -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
    layoutMode: ViewLayoutMode? = null,
    onLayoutModeChanged: ((ViewLayoutMode) -> Unit)? = null,
    accentColor: Color = AccentCyan
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$itemCount items",
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .glassmorphic(shape = RoundedCornerShape(10.dp), backgroundColor = DarkCardGlass)
                        .clickable { menuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Options",
                        tint = accentColor,
                        modifier = Modifier.padding(1.dp)
                    )
                    Text(
                        text = when (sortBy) {
                            SortBy.TITLE -> "Alphabetical"
                            SortBy.DATE_MODIFIED -> "Date Modified"
                            SortBy.DATE_ADDED -> "Date Added"
                            SortBy.DURATION -> "Duration"
                            SortBy.PLAY_COUNT -> "Play Count"
                        },
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkCardGlass)
                ) {
                    DropdownMenuItem(
                        text = { Text("Alphabetical (A-Z)", color = TextWhite) },
                        onClick = {
                            onSortByChanged(SortBy.TITLE)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Modified", color = TextWhite) },
                        onClick = {
                            onSortByChanged(SortBy.DATE_MODIFIED)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added", color = TextWhite) },
                        onClick = {
                            onSortByChanged(SortBy.DATE_ADDED)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration", color = TextWhite) },
                        onClick = {
                            onSortByChanged(SortBy.DURATION)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Count", color = TextWhite) },
                        onClick = {
                            onSortByChanged(SortBy.PLAY_COUNT)
                            menuExpanded = false
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .glassmorphic(shape = RoundedCornerShape(10.dp), backgroundColor = DarkCardGlass)
                    .clickable { onToggleDirection() }
                    .padding(6.dp)
                    .testTag("btn_toggle_sort_direction"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.ASCENDING)
                        Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort Direction",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (layoutMode != null && onLayoutModeChanged != null) {
                var layoutMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .glassmorphic(shape = RoundedCornerShape(10.dp), backgroundColor = DarkCardGlass)
                            .clickable { layoutMenuExpanded = true }
                            .padding(6.dp)
                            .testTag("btn_toggle_layout_mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (layoutMode) {
                                ViewLayoutMode.LIST_NORMAL -> Icons.Default.ViewAgenda
                                ViewLayoutMode.LIST_COMPACT -> Icons.Default.ViewHeadline
                                ViewLayoutMode.GRID_3 -> Icons.Default.GridView
                                ViewLayoutMode.GRID_4 -> Icons.Default.Apps
                            },
                            contentDescription = "View Mode",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = layoutMenuExpanded,
                        onDismissRequest = { layoutMenuExpanded = false },
                        modifier = Modifier.background(DarkCardGlass)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Normal List", color = TextWhite) },
                            onClick = {
                                onLayoutModeChanged(ViewLayoutMode.LIST_NORMAL)
                                layoutMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compact List", color = TextWhite) },
                            onClick = {
                                onLayoutModeChanged(ViewLayoutMode.LIST_COMPACT)
                                layoutMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Grid (3 Columns)", color = TextWhite) },
                            onClick = {
                                onLayoutModeChanged(ViewLayoutMode.GRID_3)
                                layoutMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Grid (4 Columns)", color = TextWhite) },
                            onClick = {
                                onLayoutModeChanged(ViewLayoutMode.GRID_4)
                                layoutMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
