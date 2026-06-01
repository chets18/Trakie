// Developer: Chetraj Jaishi
package com.example.ui

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ActivityLog
import com.example.data.Alarm
import com.example.data.Note
import com.example.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val alarms by viewModel.allAlarms.collectAsStateWithLifecycle()
    val logs by viewModel.allActivityLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Pickers states
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var isNewNoteDialogOpen by remember { mutableStateOf(false) }
    var isAlarmDialogOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // System pickers
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: Exception) {
                // persistent permission not strictly required for temporary playback
            }
            val fileName = viewModel.getFileName(context, it)
            viewModel.setImportedMusic(it.toString(), fileName)
        }
    }

    // Storage and Notification Permissions Launcher to trigger at the very beginning
    val permissionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        android.util.Log.d("trakie", "Initial permissions request results: $results (All granted = $allGranted)")
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        permissionRequestLauncher.launch(permissions)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.4f),
                                    spotColor = Color.Black.copy(alpha = 0.4f)
                                )
                                .background(
                                    color = Color(0xFF121212),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                contentDescription = "trakie Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "trakie",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isDark) 360f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                    
                    IconButton(
                        onClick = { isSettingsOpen = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "App Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val tabs = listOf(
                    Triple("Monitor", Icons.Default.Timeline, Icons.Outlined.Timeline),
                    Triple("Time Lab", Icons.Default.AccessAlarm, Icons.Outlined.AccessAlarm),
                    Triple("Notes", Icons.Default.StickyNote2, Icons.Outlined.StickyNote2)
                )

                tabs.forEachIndexed { index, item ->
                    val isSelected = activeTab == index
                    // Animated Scale/Bounce on selection
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.second else item.third,
                                contentDescription = item.first,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )
                        },
                        label = {
                            Text(
                                text = item.first,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        } else {
                            listOf(
                                Color(0xFFFBFDFF),
                                Color(0xFFF0F4F8)
                            )
                        }
                    )
                )
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ActivityMonitorTab(viewModel, logs)
                    1 -> MergedAlarmsTab(viewModel, alarms, pickAudioLauncher) { isAlarmDialogOpen = true }
                    2 -> NotesTab(
                        viewModel = viewModel,
                        notes = notes,
                        onCreateNewNote = {
                            editingNote = null
                            isNewNoteDialogOpen = true
                        },
                        onEditNote = { note ->
                            editingNote = note
                            isNewNoteDialogOpen = true
                        }
                    )
                }
            }
        }
    }

    // New/Edit Note Dialog (Rich Mini Notch Editor)
    if (isNewNoteDialogOpen) {
        NoteEditorDialog(
            note = editingNote,
            onDismiss = { isNewNoteDialogOpen = false },
            onSave = { title, content, size, style, b, i, u, img ->
                viewModel.saveNote(
                    id = editingNote?.id ?: 0,
                    title = title,
                    content = content,
                    fontSize = size,
                    fontFamily = style,
                    isBold = b,
                    isItalic = i,
                    isUnderlined = u,
                    imageUrl = img
                )
                isNewNoteDialogOpen = false
            }
        )
    }

    // New Alarm Dialog
    if (isAlarmDialogOpen) {
        AlarmCreatorDialog(
            viewModel = viewModel,
            pickAudioLauncher = pickAudioLauncher,
            onDismiss = { isAlarmDialogOpen = false },
            onSave = { hour, minute, label, repeatDays, uri, n ->
                viewModel.addAlarm(hour, minute, label, repeatDays, uri, n)
                isAlarmDialogOpen = false
            }
        )
    }

    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { isSettingsOpen = false }
        )
    }
}

@Composable
fun SettingsDialog(
    viewModel: TrackerViewModel,
    onDismiss: () -> Unit
) {
    val isMonochrome by viewModel.isMonochromeMode.collectAsStateWithLifecycle()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("App Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Personalize colors, styling, and locate resource paths.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Monochrome switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.toggleMonochromeMode() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monochrome Mode (B&W)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Display the screen design in brutalist high-contrast grayscale.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isMonochrome,
                        onCheckedChange = { viewModel.toggleMonochromeMode() }
                    )
                }

                // Info card for Logo replacement
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Logo Asset Customization", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "To customize the logo next to 'trakie', replace this file name inside your project's resource folders:\n\n• app/src/main/res/drawable/ic_launcher_foreground.xml",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// Extension to dynamically scale icons
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.drawBehind {
        // Compose scales layout bounds, this adjusts canvas drawing sizes
    }
        .sizeIn(minWidth = (24 * scale).dp, minHeight = (24 * scale).dp)
)

// ============================================================================
// TAB 1: Activity Tracker Monitor
// ============================================================================
@Composable
fun ActivityMonitorTab(
    viewModel: TrackerViewModel,
    logs: List<ActivityLog>
) {
    val activeActivity by viewModel.activeActivity.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.activeActivitySeconds.collectAsStateWithLifecycle()

    var selectedPreset by remember { mutableStateOf("Studying") }
    val presets = listOf("Studying", "Working", "Sleeping", "Traveling", "Exercising", "Meditating", "Leisure")

    val infiniteTransition = rememberInfiniteTransition(label = "RadarWave")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseWaveScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and intro
        Text(
            text = "Real-Time Activity Engine",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Monitor logs offline and preserve absolute privacy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        // Two-Column Grid: Active tracking (left) & Today's Aggregated Stats (right)
        val totalSecs = logs.sumOf { it.durationSeconds }
        val totalHours = totalSecs / 3600
        val totalMins = (totalSecs % 3600) / 60
        val formattedTotal = if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Card: Dynamic Local Session Monitor (Deep Focus Card layout)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    val isTracking = activeActivity != null
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pulsing emerald/dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) Color(0xFF10B981) else Color.LightGray)
                        )
                        Text(
                            text = if (isTracking) "TRACKING" else "IDLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = activeActivity ?: "Routines Idle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val hrs = durationSeconds / 3600
                    val mns = (durationSeconds % 3600) / 60
                    val scs = durationSeconds % 60
                    val durationText = if (isTracking) {
                        String.format("%02d:%02d:%02d", hrs, mns, scs)
                    } else "00:00:00"

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Right Card: Today's Offline Aggregation Stats
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 2.dp)
                    )
                    Text(
                        text = "Total Tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Action Panel (Start chips / Stop logs controls)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeActivity != null) {
                    // Running session action button block
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.stopAndLogActivity() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32) // Forest green
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("stop_log_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop & Log", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelActiveActivity() },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Empty state (Choose category preset and start)
                    Text(
                        text = "Select Activity Preset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    // Flow of chips to pick activity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPreset = preset },
                                label = { Text(preset) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.startActivityTracking(selectedPreset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_session_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Tracking Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Analytics Pattern breakdown
        if (logs.isNotEmpty()) {
            Text(
                text = "Tracking Analytics Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Mini physical progression maps
            val grouped = logs.groupBy { it.activityName }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(0.40f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalLogged = logs.sumOf { it.durationSeconds }
                grouped.forEach { (name, groupLogs) ->
                    val catTotal = groupLogs.sumOf { it.durationSeconds }
                    val percent = if (totalLogged > 0) catTotal.toFloat() / totalLogged else 0f
                    val formattedTime = if (catTotal >= 3600) {
                        String.format("%.1fh", catTotal.toFloat() / 3600f)
                    } else {
                        String.format("%dm", catTotal / 60)
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("$formattedTime (${String.format("%.0f%%", percent * 100)})", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = when (name) {
                                "Studying" -> Color(0xFF6200EE)
                                "Working" -> Color(0xFF03DAC6)
                                "Sleeping" -> Color(0xFF3F51B5)
                                "Exercising" -> Color(0xFFFF5722)
                                "Meditating" -> Color(0xFF2E7D32)
                                else -> Color(0xFFFF9800)
                            }
                        )
                    }
                }
            }
        }

        // History logs
        Text(
            text = "Activity Log History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.40f)
                    )
                    Text("No records found", fontWeight = FontWeight.Bold)
                    Text("Start tracking your routines to build local private logs.", fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                logs.take(15).forEach { log ->
                    val timeString = remember(log.startTime) {
                        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        sdf.format(Date(log.startTime))
                    }
                    val hrs = log.durationSeconds / 3600
                    val mns = (log.durationSeconds % 3600) / 60
                    val scs = log.durationSeconds % 60
                    val durationString = if (hrs > 0) {
                        String.format("%02dh %02dm", hrs, mns)
                    } else if (mns > 0) {
                        String.format("%02dm %02ds", mns, scs)
                    } else {
                        String.format("%ds", scs)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (log.activityName) {
                                                "Studying" -> Color(0xFFE2D6FF)
                                                "Working" -> Color(0xFFC7F9F4)
                                                "Sleeping" -> Color(0xFFC5CAE9)
                                                "Exercising" -> Color(0xFFFFCCBC)
                                                "Meditating" -> Color(0xFFC8E6C9)
                                                else -> Color(0xFFFFE0B2)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (log.activityName) {
                                            "Studying" -> Icons.Default.MenuBook
                                            "Working" -> Icons.Default.LaptopMac
                                            "Sleeping" -> Icons.Default.Bedtime
                                            "Exercising" -> Icons.Default.DirectionsRun
                                            "Meditating" -> Icons.Default.Spa
                                            else -> Icons.Default.SportsEsports
                                        },
                                        contentDescription = null,
                                        tint = when (log.activityName) {
                                            "Studying" -> Color(0xFF6200EE)
                                            "Working" -> Color(0xFF00796B)
                                            "Sleeping" -> Color(0xFF3F51B5)
                                            "Exercising" -> Color(0xFFFF5722)
                                            "Meditating" -> Color(0xFF2E7D32)
                                            else -> Color(0xFF8D6E63)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = log.activityName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = timeString,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = durationString,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteActivityLog(log.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete record",
                                        tint = MaterialTheme.colorScheme.error.copy(0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MERGED TAB: Alarm & Stopwatch & Notifier Hybrid
// ============================================================================
@Composable
fun MergedAlarmsTab(
    viewModel: TrackerViewModel,
    alarms: List<Alarm>,
    pickAudioLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onAddAlarmClick: () -> Unit
) {
    var subTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Beautiful Segmented Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf("Alarms", "Stopwatch", "Notifier")
            subTabs.forEachIndexed { index, title ->
                val isSelected = subTab == index
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { subTab = index }
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Subtabs area with beautiful transitions
        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> AlarmsTab(viewModel, alarms, onAddAlarmClick)
                1 -> StopwatchTab(viewModel, pickAudioLauncher)
                2 -> NotifierSection(viewModel)
            }
        }
    }
}

@Composable
fun NotifierSection(viewModel: TrackerViewModel) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var customTitle by remember { mutableStateOf("") }
    var selectedInterval by remember { mutableStateOf(15) } // default 15 mins
    
    val intervalOptions = listOf(
        1 to "1 Min",
        5 to "5 Mins",
        10 to "10 Mins",
        15 to "15 Mins",
        30 to "30 Mins",
        60 to "1 Hour"
    )
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Multiple Custom Reminders",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure custom notifications with unique intervals and sound alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Add Alert config Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create Custom Alert", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    placeholder = { Text("E.g., Stretch, Are you alive?, Focus check") },
                    label = { Text("Alert keyword / Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Repeating Interval", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervalOptions.forEach { (minutes, label) ->
                        val isSelected = selectedInterval == minutes
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedInterval = minutes },
                            label = { Text(label) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val finalTitle = customTitle.trim().ifEmpty { "Self Check" }
                        viewModel.addReminder(finalTitle, selectedInterval)
                        customTitle = "" // clear inputs
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Focus Alert", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Text(
            text = "Configured Alerts (${reminders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (reminders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No alerts created. Add one above!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reminders.forEach { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (reminder.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (reminder.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Repeats: Every ${reminder.intervalMinutes} mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = reminder.isEnabled,
                                    onCheckedChange = { viewModel.toggleReminder(reminder) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                    Icon(Icons.Default.DeleteOutline, "Remove Tracker", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: Alarm Manager Controls
// ============================================================================
@Composable
fun AlarmsTab(
    viewModel: TrackerViewModel,
    alarms: List<Alarm>,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personal Alarms",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Set fully standalone offline triggers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onAddClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_alarm_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Alarm")
            }
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.AccessAlarm,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f)
                    )
                    Text("No alarms active", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Click the '+' button above to schedule your first alert.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(alarm, viewModel)
                }
            }
        }
    }
}

@Composable
fun AlarmCard(alarm: Alarm, viewModel: TrackerViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    val isEnabled = alarm.isEnabled

    val containerColor = if (isEnabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isEnabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!isEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // AM/PM Indicator & Label Overview header
                    Text(
                        text = if (alarm.label.isNotEmpty()) alarm.label.uppercase() else "ALARM EVENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.75f),
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            color = contentColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (alarm.hour < 12) "AM" else "PM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Switche toggle with proper tag
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { viewModel.toggleAlarm(alarm) },
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            // Compact details capsule indicator at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days capsule active indicator
                if (alarm.daysOfWeek.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = alarm.daysOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                } else {
                    Text(
                        text = "Once-off Alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = if (isExpanded) "Hide details ▴" else "Show details ▾",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = contentColor.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sound Source Alert",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor.copy(alpha = 0.6f)
                            )
                            Text(
                                text = alarm.audioName ?: "System Default Alert",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.deleteAlarm(alarm) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (isEnabled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                }
                            )
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete alarm")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: Stopwatch System
// ============================================================================
@Composable
fun StopwatchTab(
    viewModel: TrackerViewModel,
    pickAudioLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val durationMs by viewModel.stopwatchTimeMs.collectAsStateWithLifecycle()
    val isRunning by viewModel.isStopwatchRunning.collectAsStateWithLifecycle()
    val laps by viewModel.stopwatchLaps.collectAsStateWithLifecycle()

    val bgUri by viewModel.importedMusicUri.collectAsStateWithLifecycle()
    val bgName by viewModel.importedMusicName.collectAsStateWithLifecycle()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsStateWithLifecycle()

    val durationSec = durationMs / 1000f

    // Rotation angle for dial
    val angle = (durationSec * 360f / 60f) % 360f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "High Precision Stopwatch",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Custom Visual Sweep Arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2
                val colorSchemePrimary = Color(0xFF6750A4)
                val strokeWidth = 12f

                // Outer decorative track
                drawCircle(
                    color = colorSchemePrimary.copy(0.12f),
                    radius = outerRadius - 10f,
                    style = Stroke(width = strokeWidth)
                )

                // Sweep Sweep active arc
                drawArc(
                    color = colorSchemePrimary,
                    startAngle = -90f,
                    sweepAngle = angle,
                    useCenter = false,
                    topLeft = Offset(10f, 10f),
                    size = size.copy(width = size.width - 20f, height = size.height - 20f),
                    style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round)
                )
            }

            // Digital metrics label inside
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = (durationMs / 60000) % 60
                val secs = (durationMs / 1000) % 60
                val ms = (durationMs % 1000) / 10

                Text(
                    text = String.format("%02d:%02d.%02d", mins, secs, ms),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 32.sp
                )
                Text(
                    text = "TIMER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Toggles Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            // LAP/RESET Button
            OutlinedButton(
                onClick = { if (isRunning) viewModel.lapStopwatch() else viewModel.resetStopwatch() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(if (isRunning) "Lap" else "Reset")
            }

            // START/PAUSE Button
            Button(
                onClick = { if (isRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("stopwatch_trigger")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isRunning) "Pause" else "Start")
            }
        }

        // Background Music Panel for Stopwatch (User requirement: play songs in background)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isMusicPlaying) durationSec * 60f else 0f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Background Audio Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = bgName ?: "No custom track imported",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Import Music Track",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (bgUri != null) {
                        IconButton(
                            onClick = { viewModel.toggleBackgroundMusic() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isMusicPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isMusicPlaying) Color.white else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause background track"
                            )
                        }
                    }
                }
            }
        }

        // Laps Scroll History
        if (laps.isNotEmpty()) {
            Text(
                text = "Laps",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(0.2f))
            ) {
                itemsIndexed(laps.asReversed()) { index, lapTime ->
                    val lapIndex = laps.size - index
                    val mins = (lapTime / 60000) % 60
                    val secs = (lapTime / 1000) % 60
                    val ms = (lapTime % 1000) / 10

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap $lapIndex", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format("%02d:%02d.%02d", mins, secs, ms),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index < laps.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun <T> List<T>.indexed(): List<Pair<Int, T>> = this.mapIndexed { index, item -> index to item }

private val Color.Companion.white: Color
    get() = Color.White

// ============================================================================
// TAB 3: Dual View Notes & Statistics Reports Screen
// ============================================================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesTab(
    viewModel: TrackerViewModel,
    notes: List<Note>,
    onCreateNewNote: () -> Unit,
    onEditNote: (Note) -> Unit
) {
    var notesSubTab by remember { mutableStateOf(0) } // 0 = documents list, 1 = daily stats summaries
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(searchQuery, notes) {
        if (searchQuery.isEmpty()) notes else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val logs by viewModel.allActivityLogs.collectAsStateWithLifecycle(initialValue = emptyList())

    // Confirmation dialog states for deleting a note
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var typedConfirmationText by remember { mutableStateOf("") }

    if (noteToDelete != null) {
        val currentNote = noteToDelete!!
        val cleanTitle = currentNote.title.ifEmpty { "Untitled" }
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = {
                Text(
                    text = "Confirm Deletion",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to delete this note? This action is permanent and cannot be undone.\n\nTo confirm, please type either the note's title:\n\"$cleanTitle\"\nor type the project name \"trakie\":",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = typedConfirmationText,
                        onValueChange = { typedConfirmationText = it },
                        placeholder = { Text("Confirm by typing...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                val isMatching = typedConfirmationText.trim().equals(cleanTitle.trim(), ignoreCase = true) ||
                                 typedConfirmationText.trim().equals("trakie", ignoreCase = true)
                Button(
                    onClick = {
                        viewModel.deleteNote(currentNote.id)
                        noteToDelete = null
                        typedConfirmationText = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = isMatching
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Horizontal Sub-tabs selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf("Note Documents", "Daily Logs & Stats")
            subTabs.forEachIndexed { index, title ->
                val isSelected = notesSubTab == index
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { notesSubTab = index }
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (notesSubTab == 0) {
            // Document explorer (Existing List)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rich Notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fully local rich text formatting with photo support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCreateNewNote,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_note_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Note")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search document title, body contents...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .padding(bottom = 12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f)
                        )
                        Text("No notes found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Keep private notes client-side forever.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteItemCard(
                            note = note,
                            onEdit = { onEditNote(note) },
                            onDelete = {
                                noteToDelete = note
                                typedConfirmationText = ""
                            }
                        )
                    }
                }
            }
        } else {
            // Statistics, custom daily rating logs and compare graphs view!
            var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
            val sdfDay = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val selectedDateStr = remember(selectedDateMillis) { sdfDay.format(Date(selectedDateMillis)) }
            
            var ratingVal by remember { mutableStateOf(0) }
            var sentenceSummaryText by remember { mutableStateOf("") }

            LaunchedEffect(selectedDateStr) {
                val activeRatingAndSummary = viewModel.getDailyRatingForDate(selectedDateStr)
                ratingVal = activeRatingAndSummary?.rating ?: 0
                sentenceSummaryText = activeRatingAndSummary?.oneSentenceSummary ?: ""
            }

            // Persist ratings updates
            LaunchedEffect(ratingVal, sentenceSummaryText) {
                if (ratingVal > 0 || sentenceSummaryText.isNotEmpty()) {
                    kotlinx.coroutines.delay(300) // Debounce saving with fully-qualified delay to prevent import error
                    viewModel.saveDailyRating(selectedDateStr, ratingVal, sentenceSummaryText)
                }
            }

            val logsOfDay = remember(logs, selectedDateStr) {
                logs.filter { sdfDay.format(Date(it.startTime)) == selectedDateStr }
            }

            val chronologicalLogsWithGaps = remember(logsOfDay, selectedDateMillis) {
                val result = mutableListOf<Triple<String, Long, Long>>() // name, startMillis, durationSecs
                if (logsOfDay.isEmpty()) {
                    result.add(Triple("Unaccounted Gap", selectedDateMillis, 86400L))
                    return@remember result
                }
                val sorted = logsOfDay.sortedBy { it.startTime }
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayStart = cal.timeInMillis
                val dayEnd = dayStart + 86400000L
                
                var currentPointer = dayStart
                
                sorted.forEach { log ->
                    if (log.startTime - currentPointer >= 1800000L) {
                        val gap = (log.startTime - currentPointer) / 1000
                        result.add(Triple("Unaccounted Gap", currentPointer, gap))
                    }
                    result.add(Triple(log.activityName, log.startTime, log.durationSeconds))
                    currentPointer = log.startTime + (log.durationSeconds * 1000)
                }
                
                if (dayEnd - currentPointer >= 1800000L) {
                    val gap = (dayEnd - currentPointer) / 1000
                    result.add(Triple("Unaccounted Gap", currentPointer, gap))
                }
                result
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Date Navigator
                item {
                    val formattedDate = remember(selectedDateMillis) {
                        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedDateMillis -= 24 * 60 * 60 * 1000L }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Prev Day")
                            }
                            Text(
                                text = formattedDate,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { selectedDateMillis += 24 * 60 * 60 * 1000L }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                            }
                        }
                    }
                }

                // 24 Hour Segment Visualizer
                item {
                    Daily24HourBar(logsOfDay = logsOfDay)
                }

                // Daily Evaluation score card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("How was your day? (1 - 10)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Rate and log a quick private summary.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..10).forEach { score ->
                                    val isSelected = ratingVal == score
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { ratingVal = score }
                                    ) {
                                        Text(
                                            text = score.toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = sentenceSummaryText,
                                onValueChange = { sentenceSummaryText = it },
                                placeholder = { Text("Write your one sentence summary...") },
                                label = { Text("One Sentence Summary") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Chronological items including Unaccounted spaces
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Timeline Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            
                            chronologicalLogsWithGaps.forEach { (name, start, duration) ->
                                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(start))
                                val durationStr = if (duration >= 3600L) {
                                    String.format(Locale.US, "%.1f hrs", duration / 3600f)
                                } else {
                                    "${duration / 60} mins"
                                }
                                
                                val isGap = name == "Unaccounted Gap"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = if (isGap) MaterialTheme.colorScheme.errorContainer.copy(0.2f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isGap) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isGap) "Unaccounted Free Time" else name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isGap) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Started: $timeFormat",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isGap) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = durationStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isGap) Color(0xFFEF4444) else MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Streaks & Best day highlights Card
                item {
                    val bestDayStr = remember(logs) { calculateBestFocusedDay(logs) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Stats & Streaks Highlight", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = bestDayStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Current log streak by category:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            
                            val categories = listOf("Studying", "Working", "Sleeping", "Traveling", "Exercising", "Meditating")
                            categories.forEach { cat ->
                                val streak = remember(logs) { calculateStreakForCategory(logs, cat) }
                                if (streak > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("🔥 $streak Days Streak", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Comparable comparison graph canvas!
                item {
                    MonthlyMultiLineGraph(logs = logs)
                }
            }
        }
    }
}

@Composable
fun NoteItemCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Displays optional Image attachments directly in the card cleanly!
            if (!note.imageUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray)
                        .padding(bottom = 12.dp)
                ) {
                    AsyncImage(
                        model = Uri.parse(note.imageUrl),
                        contentDescription = "Note attachment photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifEmpty { "Untitled Document" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = when (note.fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "Monospace" -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    }
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete document",
                        tint = MaterialTheme.colorScheme.error.copy(0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Body text with applied Notion desired formats
            Text(
                text = note.content,
                fontSize = note.fontSize.sp,
                fontWeight = if (note.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (note.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (note.isUnderlined) TextDecoration.Underline else TextDecoration.None,
                fontFamily = when (note.fontFamily) {
                    "Serif" -> FontFamily.Serif
                    "Monospace" -> FontFamily.Monospace
                    else -> FontFamily.SansSerif
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Visual Indicator for note customization settings
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (note.isBold) Icon(Icons.Default.FormatBold, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    if (note.isItalic) Icon(Icons.Default.FormatItalic, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    if (note.isUnderlined) Icon(Icons.Default.FormatUnderlined, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Icon(Icons.Default.TextFields, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ============================================================================
// MODAL: Rich Notion style Note Editor Creator (Full Screen, Auto-Saving)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, Boolean, Boolean, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var fontSize by remember { mutableStateOf(note?.fontSize ?: 16) }
    var fontFamily by remember { mutableStateOf(note?.fontFamily ?: "SansSerif") }
    
    var isBold by remember { mutableStateOf(note?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(note?.isItalic ?: false) }
    var isUnderlined by remember { mutableStateOf(note?.isUnderlined ?: false) }
    var imageUrl by remember { mutableStateOf<String?>(note?.imageUrl) }
    
    var isHighlighted by remember { mutableStateOf(note?.title?.startsWith("[HL]") == true || note?.content?.startsWith("[HL]") == true) }

    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: Exception) {
            }
            imageUrl = it.toString()
        }
    }

    // Debounced Auto-save
    LaunchedEffect(title, content, fontSize, fontFamily, isBold, isItalic, isUnderlined, imageUrl) {
        if (title.isEmpty() && content.isEmpty() && note == null) return@LaunchedEffect
        kotlinx.coroutines.delay(800)
        onSave(title, content, fontSize, fontFamily, isBold, isItalic, isUnderlined, imageUrl)
    }

    // Capture standard Back button mapping
    androidx.activity.compose.BackHandler {
        onSave(title, content, fontSize, fontFamily, isBold, isItalic, isUnderlined, imageUrl)
        onDismiss()
    }

    Dialog(
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        onDismissRequest = {
            onSave(title, content, fontSize, fontFamily, isBold, isItalic, isUnderlined, imageUrl)
            onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isHighlighted) Color(0xFFFEF08A).copy(alpha = 0.96f) else MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Toolbar Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSave(title, content, fontSize, fontFamily, isBold, isItalic, isUnderlined, imageUrl)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Save and Close")
                    }

                    Text(
                        text = if (note == null) "New Note (Auto-save)" else "Edit Note (Auto-save)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        IconToggleButton(
                            checked = isHighlighted,
                            onCheckedChange = { isHighlighted = it }
                        ) {
                            Icon(
                                imageVector = if (isHighlighted) Icons.Filled.BorderColor else Icons.Outlined.BorderColor,
                                contentDescription = "Highlight",
                                tint = if (isHighlighted) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image attachment")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Document Title
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { 
                            Text(
                                "Document Title",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inner Styling Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconToggleButton(checked = isBold, onCheckedChange = { isBold = it }) {
                            Icon(Icons.Default.FormatBold, null, tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconToggleButton(checked = isItalic, onCheckedChange = { isItalic = it }) {
                            Icon(Icons.Default.FormatItalic, null, tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconToggleButton(checked = isUnderlined, onCheckedChange = { isUnderlined = it }) {
                            Icon(Icons.Default.FormatUnderlined, null, tint = if (isUnderlined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "${fontSize}sp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    fontSize = when (fontSize) {
                                        14 -> 18
                                        18 -> 24
                                        else -> 14
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        )

                        Text(
                            text = fontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    fontFamily = when (fontFamily) {
                                        "SansSerif" -> "Serif"
                                        "Serif" -> "Monospace"
                                        else -> "SansSerif"
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        )

                        // BULLET LIST HOOKS
                        Button(
                            onClick = {
                                content = if (content.endsWith("\n") || content.isEmpty()) {
                                    content + "• "
                                } else {
                                    content + "\n• "
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(4.dp))
                            Text("List", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                    if (imageUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = Uri.parse(imageUrl),
                                contentDescription = "Photo Attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUrl = null },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(0.6f)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Body
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                "Body text goes here...",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = fontSize.sp,
                                    fontFamily = when (fontFamily) {
                                        "Serif" -> FontFamily.Serif
                                        "Monospace" -> FontFamily.Monospace
                                        else -> FontFamily.SansSerif
                                    }
                                )
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = fontSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None,
                            fontFamily = when (fontFamily) {
                                "Serif" -> FontFamily.Serif
                                "Monospace" -> FontFamily.Monospace
                                else -> FontFamily.SansSerif
                            },
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp)
                            .testTag("note_body_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                // FOOTER
                val wordCount = remember(content) {
                    content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                }
                val charCount = content.length

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words  |  $charCount characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Changes Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Wrapper TextStyle fallback definition
private typealias TextStyle = androidx.compose.ui.text.TextStyle

// ============================================================================
// MODAL: Precise Alarm Creator configuration dialogue
// ============================================================================
@Composable
fun AlarmCreatorDialog(
    viewModel: TrackerViewModel,
    pickAudioLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, String, String?, String?) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(8) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    var alarmLabel by remember { mutableStateOf("") }
    
    // Default repeat elements
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf<String>() }

    val context = LocalContext.current
    val customUri by viewModel.importedMusicUri.collectAsStateWithLifecycle()
    val customName by viewModel.importedMusicName.collectAsStateWithLifecycle()

    val timePicker = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedHour = hour
            selectedMinute = minute
        },
        8,
        30,
        true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Set Local Alarm",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Clock Trigger Picker Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePicker.show() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.35f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%02d : %02d", selectedHour, selectedMinute),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap to Adjust Time",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Label field
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("Alarm Label / Event") },
                    placeholder = { Text("e.g. Gym Session, Meditation, Sleep etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alarm_label_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Repeat Days Selection
                Text("Repeat Alerts on", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { day ->
                        val active = selectedDays.contains(day)
                        Text(
                            text = day,
                            modifier = Modifier
                                .clickable {
                                    if (active) selectedDays.remove(day) else selectedDays.add(day)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Custom Audio Picker field
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sound Tone Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                customName ?: "System Default Tone",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Pick Song", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val repeatStr = selectedDays.joinToString(",")
                        onSave(selectedHour, selectedMinute, alarmLabel, repeatStr, customUri, customName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("alarm_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Schedule Alarm", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Daily24HourBar(logsOfDay: List<ActivityLog>) {
    val segments = remember(logsOfDay) {
        val colors = MutableList(24) { Color(0xFF64748B) } // Default inactive slate
        
        logsOfDay.forEach { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.startTime }
            val startHour = cal.get(Calendar.HOUR_OF_DAY)
            val durationHours = (log.durationSeconds / 3600f).coerceAtLeast(1f).toInt()
            
            for (h in startHour until (startHour + durationHours).coerceAtMost(24)) {
                colors[h] = when (log.activityName) {
                    "Studying" -> Color(0xFF6366F1)
                    "Working" -> Color(0xFF10B981)
                    "Sleeping" -> Color(0xFF3B82F6)
                    "Traveling" -> Color(0xFFEC4899)
                    "Exercising" -> Color(0xFFF59E0B)
                    "Meditating" -> Color(0xFF8B5CF6)
                    else -> Color(0xFF14B8A6)
                }
            }
        }
        colors
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "24-Hour Active Segment Bar",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            segments.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("12 AM", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            Text("6 AM", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            Text("12 PM", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            Text("6 PM", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            Text("12 AM", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Legends
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Studying", "Working", "Sleeping", "Traveling", "Exercising", "Meditating").forEach { cat ->
                val col = when (cat) {
                    "Studying" -> Color(0xFF6366F1)
                    "Working" -> Color(0xFF10B981)
                    "Sleeping" -> Color(0xFF3B82F6)
                    "Traveling" -> Color(0xFFEC4899)
                    "Exercising" -> Color(0xFFF59E0B)
                    "Meditating" -> Color(0xFF8B5CF6)
                    else -> Color(0xFF14B8A6)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cat, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun MonthlyMultiLineGraph(logs: List<ActivityLog>) {
    val categories = listOf("Studying", "Working", "Sleeping", "Traveling", "Exercising", "Meditating")
    
    val pointsBySub = remember(logs) {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        
        val past30Days = (0..29).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            sdf.format(c.time)
        }.reversed()
        
        val pointsRaw = categories.associateWith { cat ->
            val catLogs = logs.filter { it.activityName == cat }
            val groupedByDay = catLogs.groupBy { 
                val c = Calendar.getInstance().apply { timeInMillis = it.startTime }
                sdf.format(c.time)
            }
            
            past30Days.map { day ->
                val dayLogs = groupedByDay[day] ?: emptyList()
                dayLogs.sumOf { it.durationSeconds } / 3600f
            }
        }
        Triple(past30Days, pointsRaw, logs.size)
    }
    
    val dataByCat = pointsBySub.second
    
    val categoryColors = mapOf(
        "Studying" to Color(0xFF6366F1),
        "Working" to Color(0xFF10B981),
        "Sleeping" to Color(0xFF3B82F6),
        "Traveling" to Color(0xFFEC4899),
        "Exercising" to Color(0xFFF59E0B),
        "Meditating" to Color(0xFF8B5CF6)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Comparison Lines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Hours logged per activity over the past 30 days.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 40f
                    val paddingRight = 16f
                    val paddingTop = 16f
                    val paddingBottom = 32f
                    
                    val graphWidth = width - paddingLeft - paddingRight
                    val graphHeight = height - paddingTop - paddingBottom
                    
                    val yLabels = listOf(0f, 4f, 8f, 12f, 16f)
                    val maxY = 16f
                    
                    yLabels.forEach { hours ->
                        val y = paddingTop + graphHeight * (1f - (hours / maxY))
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    categories.forEach { cat ->
                        val color = categoryColors[cat] ?: Color.Gray
                        val hoursList = dataByCat[cat] ?: List(30) { 0f }
                        
                        val points = hoursList.mapIndexed { index, hour ->
                            val x = paddingLeft + (index / 29f) * graphWidth
                            val y = paddingTop + graphHeight * (1f - (hour.coerceAtMost(maxY) / maxY))
                            Offset(x, y)
                        }
                        
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = color,
                                start = points[i],
                                end = points[i+1],
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, paddingTop),
                        end = Offset(paddingLeft, height - paddingBottom),
                        strokeWidth = 2f
                    )
                    
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, height - paddingBottom),
                        end = Offset(width - paddingRight, height - paddingBottom),
                        strokeWidth = 2f
                    )
                }
            }
            
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("30 Days Ago", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("15 Days Ago", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("Today", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

fun calculateBestFocusedDay(logs: List<ActivityLog>): String {
    val cal = Calendar.getInstance()
    val rawMap = logs.groupBy { 
        cal.timeInMillis = it.startTime
        cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
    }
    
    var maxSeconds = 0L
    var bestDayStr = ""
    val sdf = SimpleDateFormat("MMMM d", Locale.getDefault())
    
    rawMap.forEach { (key, group) ->
        val totalSecs = group.sumOf { it.durationSeconds }
        if (totalSecs > maxSeconds) {
            maxSeconds = totalSecs
            val firstLog = group.first()
            bestDayStr = sdf.format(Date(firstLog.startTime))
        }
    }
    
    val totalHrsFormat = String.format(Locale.US, "%.1f", maxSeconds / 3600f)
    return if (maxSeconds > 0) {
        "Your best day was $bestDayStr with $totalHrsFormat hours tracked."
    } else {
        "Start tracking activities to calculate your best focused day!"
    }
}

fun calculateStreakForCategory(logs: List<ActivityLog>, category: String): Int {
    val catLogs = logs.filter { it.activityName == category }
    if (catLogs.isEmpty()) return 0
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = catLogs.map { sdf.format(Date(it.startTime)) }.toSet()
    
    var streak = 0
    val cal = Calendar.getInstance()
    
    while (true) {
        val dateStr = sdf.format(cal.time)
        if (dates.contains(dateStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            if (streak == 0) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = sdf.format(cal.time)
                if (dates.contains(yesterdayStr)) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    continue
                }
            }
            break
        }
    }
    return streak
}
