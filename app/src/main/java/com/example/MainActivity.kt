package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.ui.components.MiniPlayer
import com.example.ui.components.QueueSheet
import com.example.ui.components.TabBar
import com.example.ui.dialogs.EqualizerDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.dialogs.TagEditorDialog
import com.example.ui.lockscreen.LockScreenActivity
import com.example.ui.screens.ArtistsTab
import com.example.ui.screens.DustyTracksTab
import com.example.ui.screens.FavoritesTab
import com.example.ui.screens.FoldersTab
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsTab
import com.example.ui.screens.SearchTab
import com.example.ui.screens.SongsTab
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.TabType

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.AppLogger.init(applicationContext)
        com.example.util.AppLogger.i("MainActivity", "App onCreate called")
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current

    // Permissions check
    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            @Suppress("DEPRECATION")
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val audioGranted = permissionsMap[Manifest.permission.READ_MEDIA_AUDIO] ?: permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        if (audioGranted) {
            viewModel.refreshMedia()
        }
    }

    LaunchedEffect(Unit) {
        val needRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
    val songForTagEditor by viewModel.songForTagEditor.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    val isTabBarAtTop = (settings?.tabBarPosition == "TOP")

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var tabBarHeightPx by remember { mutableIntStateOf(0) }
    val tabBarHeightDp = with(density) { tabBarHeightPx.toDp() }

    val contentBottomPadding = if (isTabBarAtTop) {
        maxOf(navBarBottom, imeBottom)
    } else {
        maxOf(tabBarHeightDp, imeBottom)
    }

    val tabList by viewModel.visibleTabs.collectAsState()
    val dialogBlurRadius by viewModel.dialogBlurRadius.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = tabList.indexOf(selectedTab).coerceAtLeast(0),
        pageCount = { tabList.size.coerceAtLeast(1) }
    )
    val coroutineScope = rememberCoroutineScope()
    val pagerOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction

    LaunchedEffect(tabList) {
        if (selectedTab !in tabList && tabList.isNotEmpty()) {
            viewModel.selectedTab.value = tabList.first()
        }
    }

    LaunchedEffect(selectedTab) {
        val targetIdx = tabList.indexOf(selectedTab)
        if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIdx)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val tab = tabList.getOrNull(pagerState.currentPage)
        if (tab != null && tab != viewModel.selectedTab.value) {
            viewModel.selectedTab.value = tab
        }
    }

    val isAnyDialogOpen = isSettingsOpen ||
            songForTagEditor != null ||
            isEqualizerOpen ||
            isQueueOpen ||
            isNowPlayingExpanded

    // Dynamic Back Navigation Handler: Navigate back through stack instead of exiting app
    BackHandler(
        enabled = isAnyDialogOpen ||
                selectedFolder != null ||
                selectedArtist != null ||
                selectedPlaylist != null ||
                selectedTab != TabType.SONGS
    ) {
        when {
            isSettingsOpen -> viewModel.isSettingsOpen.value = false
            songForTagEditor != null -> viewModel.songForTagEditor.value = null
            isEqualizerOpen -> viewModel.isEqualizerOpen.value = false
            isQueueOpen -> viewModel.isQueueOpen.value = false
            isNowPlayingExpanded -> viewModel.isNowPlayingExpanded.value = false
            selectedFolder != null -> viewModel.selectedFolder.value = null
            selectedArtist != null -> viewModel.selectedArtist.value = null
            selectedPlaylist != null -> viewModel.selectedPlaylist.value = null
            selectedTab != TabType.SONGS -> {
                viewModel.selectedTab.value = TabType.SONGS
                coroutineScope.launch {
                    val songsIdx = tabList.indexOf(TabType.SONGS)
                    if (songsIdx >= 0) pagerState.animateScrollToPage(songsIdx)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        containerColor = AmoledBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
        ) {
            // Ambient Top Color Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                dynamicAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )

            val blurModifier = if (isAnyDialogOpen && dialogBlurRadius > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(dialogBlurRadius.dp)
            } else Modifier

            // Main Tabs & Navigation Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isTabBarAtTop) 0.dp else innerPadding.calculateTopPadding(),
                        bottom = contentBottomPadding
                    )
                    .then(blurModifier)
            ) {
                // If Tab Bar is configured at TOP:
                if (isTabBarAtTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabBar(
                            selectedTab = selectedTab,
                            tabOrder = tabList,
                            onTabSelected = {
                                viewModel.selectedTab.value = it
                                coroutineScope.launch { pagerState.animateScrollToPage(tabList.indexOf(it)) }
                            },
                            onReorderTabs = { from, to ->
                                viewModel.reorderTabs(from, to)
                            },
                            isTopPlaced = true,
                            dynamicAccentColor = dynamicAccent,
                            pagerOffset = pagerOffset,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.isSettingsOpen.value = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .glassmorphic(shape = RoundedCornerShape(12.dp), backgroundColor = DarkCardGlass)
                                .testTag("btn_top_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Top header with Prism music on left and Settings button on top right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prism music",
                            color = TextWhite,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        IconButton(
                            onClick = { viewModel.isSettingsOpen.value = true },
                            modifier = Modifier
                                .size(38.dp)
                                .glassmorphic(shape = RoundedCornerShape(12.dp), backgroundColor = DarkCardGlass)
                                .testTag("btn_top_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Active Tab Screen Content with Swipe Gestures
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (tabList[page]) {
                        TabType.PLAYLISTS -> PlaylistsTab(viewModel)
                        TabType.SONGS -> SongsTab(viewModel)
                        TabType.FOLDERS -> FoldersTab(viewModel)
                        TabType.ARTISTS -> ArtistsTab(viewModel)
                        TabType.SEARCH -> SearchTab(viewModel, isTabBarAtTop = isTabBarAtTop)
                        TabType.FAVORITES -> FavoritesTab(viewModel)
                        TabType.DUSTY_TRACKS -> DustyTracksTab(viewModel)
                    }
                }

                // Mini Player (above tabs when keyboard closed, or dynamically above keyboard when keyboard opens!)
                MiniPlayer(viewModel = viewModel)
            }

            // If Tab Bar is configured at BOTTOM: pinned to bottom of screen so keyboard covers it
            if (!isTabBarAtTop) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            tabBarHeightPx = coordinates.size.height
                        }
                        .then(blurModifier)
                ) {
                    TabBar(
                        selectedTab = selectedTab,
                        tabOrder = tabList,
                        onTabSelected = {
                            viewModel.selectedTab.value = it
                            coroutineScope.launch { pagerState.animateScrollToPage(tabList.indexOf(it)) }
                        },
                        onReorderTabs = { from, to ->
                            viewModel.reorderTabs(from, to)
                        },
                        isTopPlaced = false,
                        dynamicAccentColor = dynamicAccent,
                        pagerOffset = pagerOffset
                    )
                }
            }

            if (isAnyDialogOpen && dialogBlurRadius > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (dialogBlurRadius / 70f).coerceIn(0.15f, 0.65f)))
                )
            }

            // MANDATE: Now Playing Screen (Default State: App opens directly to 'Now Playing'
            // with the last played track loaded (paused). Swipe down transitions to Mini Player.)
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                NowPlayingScreen(viewModel = viewModel)
            }

            // Queue Sheet (Swipe up on Now Playing opens Queue)
            QueueSheet(
                viewModel = viewModel,
                isOpen = isQueueOpen,
                onClose = { viewModel.isQueueOpen.value = false }
            )

            // Equalizer Dialog
            if (isEqualizerOpen) {
                EqualizerDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.isEqualizerOpen.value = false }
                )
            }

            // ID3 Tag Editor Dialog
            songForTagEditor?.let { song ->
                TagEditorDialog(
                    song = song,
                    viewModel = viewModel,
                    onDismiss = { viewModel.songForTagEditor.value = null }
                )
            }

            // Preferences & Settings Screen (Full categorized screen)
            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.isSettingsOpen.value = false }
                )
            }
        }
    }
}

