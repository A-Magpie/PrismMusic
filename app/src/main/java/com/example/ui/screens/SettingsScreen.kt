package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.util.AppLogger
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.ui.components.SleekSliderTrack
import com.example.ui.components.SolidCircleThumb
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.TabType
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsState()
    val current = settings ?: AppSettingsEntity()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val selectedFolders by viewModel.selectedFolders.collectAsState()
    val detectedFolders by viewModel.detectedFolders.collectAsState()
    val rootFolders by viewModel.rootFolders.collectAsState()
    val excludedFolders by viewModel.excludedFolders.collectAsState()
    val scanSummary by viewModel.scanSummaryState.collectAsState()
    val dialogBlurRadius by viewModel.dialogBlurRadius.collectAsState()
    val enabledTabs by viewModel.enabledTabs.collectAsState()
    val m3uImportMessage by viewModel.m3uImportMessage.collectAsState()

    var tabBarPos by remember(current.tabBarPosition) { mutableStateOf(current.tabBarPosition) }
    var widgetOpacity by remember(current.widgetOpacity) { mutableFloatStateOf(current.widgetOpacity) }
    var lockScreenEnabled by remember(current.lockScreenEnabled) { mutableStateOf(current.lockScreenEnabled) }
    var touchLockDurationMs by remember(current.touchLockDurationMs) { mutableFloatStateOf(current.touchLockDurationMs.toFloat()) }
    var lockDisableSeekbar by remember(current.lockDisableSeekbar) { mutableStateOf(current.lockDisableSeekbar) }
    var lockDisablePrevNext by remember(current.lockDisablePrevNext) { mutableStateOf(current.lockDisablePrevNext) }
    var lockDisablePlayPause by remember(current.lockDisablePlayPause) { mutableStateOf(current.lockDisablePlayPause) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showLogViewerDialog by remember { mutableStateOf(false) }
    var logContentText by remember { mutableStateOf("") }

    // Folder picker launcher (Manual Folder Selection)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = getPathFromUri(context, uri)
            viewModel.addSelectedFolder(path)
        }
    }

    val scrollState = rememberScrollState()

    fun saveAll() {
        viewModel.updateSettings(
            current.copy(
                tabBarPosition = tabBarPos,
                widgetOpacity = widgetOpacity,
                widgetTransparent = widgetOpacity < 0.95f,
                lockScreenEnabled = lockScreenEnabled,
                touchLockDurationMs = touchLockDurationMs.toLong(),
                lockDisableSeekbar = lockDisableSeekbar,
                lockDisablePrevNext = lockDisablePrevNext,
                lockDisablePlayPause = lockDisablePlayPause
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        saveAll()
                        onBack()
                    },
                    modifier = Modifier.testTag("btn_settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Preferences & Settings",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CATEGORY 1: Appearance & Widget
                SettingsCategoryCard(
                    title = "Appearance & Layout",
                    icon = Icons.Default.Palette,
                    accentColor = dynamicAccent
                ) {
                    // Tab Bar Position
                    Text(
                        text = "Tab Navigation Position",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                tabBarPos = "top"
                                saveAll()
                            }
                        ) {
                            RadioButton(
                                selected = (tabBarPos == "top"),
                                onClick = {
                                    tabBarPos = "top"
                                    saveAll()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = dynamicAccent)
                            )
                            Text("Top", color = TextWhite, fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                tabBarPos = "bottom"
                                saveAll()
                            }
                        ) {
                            RadioButton(
                                selected = (tabBarPos == "bottom"),
                                onClick = {
                                    tabBarPos = "bottom"
                                    saveAll()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = dynamicAccent)
                            )
                            Text("Bottom", color = TextWhite, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Widget Background Transparency Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = dynamicAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Widget Background Opacity",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (widgetOpacity <= 0.02f) "0% (Transparent)" else "${(widgetOpacity * 100).roundToInt()}%",
                            color = dynamicAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Slide to 0% to make the widget background completely transparent",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Slider(
                        value = widgetOpacity,
                        onValueChange = {
                            widgetOpacity = it
                            saveAll()
                        },
                        valueRange = 0.0f..1.0f,
                        track = { sliderState ->
                            SleekSliderTrack(
                                sliderState = sliderState,
                                activeTrackColor = dynamicAccent,
                                inactiveTrackColor = Color(0x33FFFFFF),
                                trackHeight = 4.dp
                            )
                        },
                        thumb = {
                            SolidCircleThumb(color = dynamicAccent, size = 16.dp)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Dialog Background Blur Intensity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = dynamicAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dialog Background Blur",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${dialogBlurRadius.roundToInt()} dp",
                            color = dynamicAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Adjust the background blur intensity applied beneath dialogs and sheets (0 to 30 dp)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Slider(
                        value = dialogBlurRadius,
                        onValueChange = {
                            viewModel.setDialogBlurRadius(it)
                        },
                        valueRange = 0f..30f,
                        track = { sliderState ->
                            SleekSliderTrack(
                                sliderState = sliderState,
                                activeTrackColor = dynamicAccent,
                                inactiveTrackColor = Color(0x33FFFFFF),
                                trackHeight = 4.dp
                            )
                        },
                        thumb = {
                            SolidCircleThumb(color = dynamicAccent, size = 16.dp)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // CATEGORY: Navigation Tabs Visibility
                SettingsCategoryCard(
                    title = "Navigation Tabs Visibility",
                    icon = Icons.Default.Widgets,
                    accentColor = dynamicAccent
                ) {
                    Text(
                        text = "Toggle tabs displayed in the bottom navigation bar. Disabling a tab removes it and redistributes remaining tabs evenly.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    TabType.values().forEach { tab ->
                        val isEnabled = tab in enabledTabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isEnabled || enabledTabs.size > 1) { viewModel.toggleTabEnabled(tab) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.label,
                                color = if (isEnabled) TextWhite else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                            )
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.toggleTabEnabled(tab) },
                                colors = SwitchDefaults.colors(checkedThumbColor = dynamicAccent),
                                enabled = !isEnabled || enabledTabs.size > 1
                            )
                        }
                    }
                }

                // CATEGORY 2: Lock Screen & Security
                SettingsCategoryCard(
                    title = "Lock Screen & Protection",
                    icon = Icons.Default.Security,
                    accentColor = dynamicAccent
                ) {
                    // Lock screen enable toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show on Lock Screen",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Full-screen lock with gesture swipe-to-unlock",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = lockScreenEnabled,
                            onCheckedChange = {
                                lockScreenEnabled = it
                                saveAll()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = dynamicAccent)
                        )
                    }

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Touch Lock Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Touch Lock Hold Duration",
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "%.1f s".format(touchLockDurationMs / 1000f),
                            color = dynamicAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = touchLockDurationMs,
                        onValueChange = {
                            touchLockDurationMs = it
                            saveAll()
                        },
                        valueRange = 500f..3000f,
                        steps = 5,
                        track = { sliderState ->
                            SleekSliderTrack(
                                sliderState = sliderState,
                                activeTrackColor = dynamicAccent,
                                inactiveTrackColor = Color(0x33FFFFFF),
                                trackHeight = 4.dp
                            )
                        },
                        thumb = {
                            SolidCircleThumb(color = dynamicAccent, size = 16.dp)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Lock Screen Restrictions",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Checkbox: Disable Seekbar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockDisableSeekbar = !lockDisableSeekbar
                                saveAll()
                            }
                    ) {
                        Checkbox(
                            checked = lockDisableSeekbar,
                            onCheckedChange = {
                                lockDisableSeekbar = it
                                saveAll()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = dynamicAccent)
                        )
                        Text("Disable Seekbar during Lock", color = TextWhite, fontSize = 13.sp)
                    }

                    // Checkbox: Disable Prev/Next
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockDisablePrevNext = !lockDisablePrevNext
                                saveAll()
                            }
                    ) {
                        Checkbox(
                            checked = lockDisablePrevNext,
                            onCheckedChange = {
                                lockDisablePrevNext = it
                                saveAll()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = dynamicAccent)
                        )
                        Text("Disable Prev / Next buttons during Lock", color = TextWhite, fontSize = 13.sp)
                    }

                    // Checkbox: Disable Play/Pause
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockDisablePlayPause = !lockDisablePlayPause
                                saveAll()
                            }
                    ) {
                        Checkbox(
                            checked = lockDisablePlayPause,
                            onCheckedChange = {
                                lockDisablePlayPause = it
                                saveAll()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = dynamicAccent)
                        )
                        Text("Disable Play / Pause button during Lock", color = TextWhite, fontSize = 13.sp)
                    }
                }

                // CATEGORY 3: Library & Folders
                SettingsCategoryCard(
                    title = "Library & Folders",
                    icon = Icons.Default.FolderOpen,
                    accentColor = dynamicAccent
                ) {
                    Text(
                        text = "Manage music source directories for your library. By default, no folders are scanned.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = dynamicAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Folder", color = TextWhite, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.refreshMedia() },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Active Music Directories (${selectedFolders.size})",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (selectedFolders.isEmpty()) {
                        Text(
                            text = "No directories selected yet. Tap '+ Add Folder' above to select your music folders.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedFolders.forEach { folder ->
                                val trackCount = allSongs.count { it.folderPath.startsWith(folder, ignoreCase = true) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = dynamicAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder,
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$trackCount tracks found",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeSelectedFolder(folder) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Folder",
                                            tint = AccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Playlists (.m3u / .m3u8)",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Scan all device storage to automatically discover and import M3U / M3U8 playlist files.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.autoScanAndImportAllPlaylists() },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Scan All Playlists", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (m3uImportMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = m3uImportMessage ?: "",
                            color = dynamicAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // App Permissions Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manage Android Permissions",
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                        Text("Open Settings →", color = dynamicAccent, fontSize = 12.sp)
                    }
                }

                // CATEGORY 4: Folder Exclusion (Hierarchical Tree View)
                SettingsCategoryCard(
                    title = "Folder Exclusion",
                    icon = Icons.Default.Folder,
                    accentColor = dynamicAccent
                ) {
                    Text(
                        text = "Expand directories to uncheck and exclude specific folders or subfolders from indexing.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    val allDiscovered = remember(detectedFolders, selectedFolders) {
                        (detectedFolders + selectedFolders).filter { it.isNotBlank() }.distinct().sorted()
                    }
                    val folderTree = remember(allDiscovered) {
                        buildFolderTree(allDiscovered)
                    }

                    if (folderTree.isEmpty()) {
                        Text(
                            text = "No music folders detected yet. Add folders above first.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            folderTree.forEach { node ->
                                FolderTreeNodeItem(
                                    node = node,
                                    depth = 0,
                                    excludedFolders = excludedFolders,
                                    onToggleExclude = { path -> viewModel.toggleExcludeFolder(path) },
                                    accentColor = dynamicAccent
                                )
                            }
                        }
                    }
                }

                // CATEGORY 4: Audio Equalizer & FX
                SettingsCategoryCard(
                    title = "Audio Engine",
                    icon = Icons.Default.Equalizer,
                    accentColor = dynamicAccent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.isEqualizerOpen.value = true
                                onBack()
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "10-Band Pro Equalizer & FX",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Bass Boost, Virtualizer, Reverb, Pitch & Speed",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = dynamicAccent
                        )
                    }
                }

                // CATEGORY: Diagnostics & System Logging
                val isLoggingEnabled by AppLogger.isLoggingEnabled.collectAsState()
                var showLogViewerDialog by remember { mutableStateOf(false) }
                var logContentText by remember { mutableStateOf("") }
                var showClearConfirmDialog by remember { mutableStateOf(false) }

                SettingsCategoryCard(
                    title = "Diagnostics & App Logging",
                    icon = Icons.Default.BugReport,
                    accentColor = dynamicAccent
                ) {
                    Text(
                        text = "Record detailed app events, player state, background scanner, and unexpected errors to internal storage for troubleshooting.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable App Logging", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (isLoggingEnabled) "Logging active to internal file" else "Logging is currently disabled", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isLoggingEnabled,
                            onCheckedChange = { AppLogger.setLoggingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = dynamicAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                logContentText = AppLogger.getLogText(context, 1000)
                                showLogViewerDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View Logs", color = TextWhite, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val logFile = AppLogger.getLogFile(context)
                                if (logFile != null && logFile.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        logFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Prism Music Logs"))
                                } else {
                                    val text = AppLogger.getLogText(context, 500)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Prism Music Logs"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = AccentRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // We handle dialogs below
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = DarkCardGlass,
                shape = RoundedCornerShape(16.dp),
                title = { Text("Clear All Logs?", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently delete saved debug and error log records from this device.", color = TextMuted, fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            AppLogger.clearLogs(context)
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Clear", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel", color = TextWhite)
                    }
                }
            )
        }

        if (showLogViewerDialog) {
            AlertDialog(
                onDismissRequest = { showLogViewerDialog = false },
                containerColor = DarkCardGlass,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Logs", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = {
                            logContentText = AppLogger.getLogText(context, 1000)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = dynamicAccent)
                        }
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D0D10))
                            .padding(10.dp)
                    ) {
                        val logScrollState = rememberScrollState()
                        Text(
                            text = logContentText.ifBlank { "Log is currently empty." },
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(logScrollState)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showLogViewerDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (scanSummary != null) {
            val summary = scanSummary!!
            val msg = if (summary.addedCount > 0 || summary.deletedCount > 0) {
                "Scan completed successfully.\n\n• New tracks found: ${summary.addedCount}\n• Tracks removed: ${summary.deletedCount}\n• Total tracks in library: ${summary.totalCount}"
            } else {
                "Scan completed.\nYour library is up to date and no file changes were detected (${summary.totalCount} tracks)."
            }

            AlertDialog(
                onDismissRequest = { viewModel.scanSummaryState.value = null },
                confirmButton = {
                    Button(
                        onClick = { viewModel.scanSummaryState.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("Media Scan Result", color = TextWhite, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(msg, color = TextWhite, fontSize = 14.sp)
                },
                containerColor = DarkCardGlass,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "category_expand_arrow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(18.dp), backgroundColor = DarkCardGlass)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = rotationState)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

private fun getPathFromUri(context: android.content.Context, uri: Uri): String {
    try {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        return if ("primary".equals(type, ignoreCase = true)) {
            if (split.size > 1) {
                "/storage/emulated/0/" + split[1]
            } else {
                "/storage/emulated/0"
            }
        } else {
            if (split.size > 1) {
                "/storage/$type/" + split[1]
            } else {
                "/storage/$type"
            }
        }
    } catch (_: Exception) {
        return uri.path ?: uri.toString()
    }
}

private data class FolderTreeNode(
    val name: String,
    val fullPath: String,
    val children: MutableList<FolderTreeNode> = mutableListOf()
)

private fun buildFolderTree(paths: List<String>): List<FolderTreeNode> {
    val rootNodes = mutableListOf<FolderTreeNode>()
    for (path in paths) {
        val normalized = path.trim().removePrefix("/").removeSuffix("/")
        if (normalized.isBlank()) continue
        val parts = normalized.split("/")
        var currentChildren = rootNodes
        var currentPath = ""
        for (part in parts) {
            currentPath = if (currentPath.isEmpty()) "/$part" else "$currentPath/$part"
            var node = currentChildren.find { it.name == part }
            if (node == null) {
                node = FolderTreeNode(name = part, fullPath = currentPath)
                currentChildren.add(node)
            }
            currentChildren = node.children
        }
    }
    return rootNodes
}

@Composable
private fun FolderTreeNodeItem(
    node: FolderTreeNode,
    depth: Int,
    excludedFolders: Set<String>,
    onToggleExclude: (String) -> Unit,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(depth < 2) }
    val isExcluded = excludedFolders.contains(node.fullPath)
    val hasChildren = node.children.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp)
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(rotationZ = if (isExpanded) 0f else -90f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isExcluded) TextMuted else accentColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = node.name,
                color = if (isExcluded) TextMuted else TextWhite,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Checkbox(
                checked = !isExcluded,
                onCheckedChange = { onToggleExclude(node.fullPath) },
                colors = CheckboxDefaults.colors(checkedColor = accentColor),
                modifier = Modifier.size(24.dp)
            )
        }

        if (hasChildren && isExpanded) {
            node.children.forEach { child ->
                FolderTreeNodeItem(
                    node = child,
                    depth = depth + 1,
                    excludedFolders = excludedFolders,
                    onToggleExclude = onToggleExclude,
                    accentColor = accentColor
                )
            }
        }
    }
}
