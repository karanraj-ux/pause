package com.example
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import com.example.ui.screens.*

import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.data.SmsLogEntity
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import com.example.data.repository.SettingsRepository

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val container = (application as ShieldApplication).container
        MainViewModel.Factory(container.smsRepository, container.financialRepository, container.ruleRepository, container.scheduledTaskRepository)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        if (intent?.getBooleanExtra("BLOCK_CALL", false) == true) {
            android.widget.Toast.makeText(this, "Spam Caller Blocked & Reported!", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        enableEdgeToEdge()
        setContent {
            val settingsRepository = (applicationContext as ShieldApplication).container.settingsRepository
            val appThemeFlow = settingsRepository.appTheme
            val appThemeState = appThemeFlow.collectAsState(initial = "system")
            val darkTheme = when (appThemeState.value) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)
                
                val onboardingCompleteFlow = settingsRepository.getBoolean(SettingsRepository.ONBOARDING_COMPLETE, false)
                val hasSeenWelcomeFlow = settingsRepository.getBoolean(SettingsRepository.HAS_SEEN_WELCOME, false)
                
                val isReadyState = onboardingCompleteFlow.collectAsState(initial = null)
                val hasSeenWelcomeState = hasSeenWelcomeFlow.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                
                var initialRoute by remember { mutableStateOf(Screen.Dashboard.route) }
                
                when {
                    isReadyState.value == null || hasSeenWelcomeState.value == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8F9FA))
                        )
                    }
                    hasSeenWelcomeState.value == false -> {
                        com.example.ui.screens.WelcomeOnboardingScreen(onComplete = { 
                            scope.launch {
                                settingsRepository.updateBoolean(SettingsRepository.HAS_SEEN_WELCOME, true)
                            }
                        })
                    }
                    isReadyState.value == false -> {
                        com.example.ui.screens.AssistantOnboardingScreen(onComplete = { tabRoute ->
                            scope.launch {
                                if (tabRoute != null) {
                                    initialRoute = tabRoute
                                }
                                settingsRepository.updateBoolean(SettingsRepository.ONBOARDING_COMPLETE, true)
                            }
                        })
                    }
                    else -> {
                        MainScreen(viewModel, windowSizeClass.widthSizeClass, startDestination = initialRoute)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == "com.example.ACTION_DIAL_NUMBER") {
            val number = intent.getStringExtra("DIAL_NUMBER")
            if (number != null) {
                try {
                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:$number")
                    }
                    startActivity(dialIntent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to start dialer", e)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Protect : Screen("shield", "Shield", Icons.Default.Security)
    object Connect : Screen("connect", "Connect", Icons.Default.ChatBubble)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.Schedule)
    object Automation : Screen("automation", "Automations", Icons.Default.Link)
    object AddSchedule : Screen("add_schedule", "Add Schedule", Icons.Default.Schedule)
    object FocusCalendar : Screen("focus_calendar", "Schedule & Focus", Icons.Default.Event)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val LocalSnackbarHostState = androidx.compose.runtime.compositionLocalOf<androidx.compose.material3.SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, widthSizeClass: WindowWidthSizeClass, startDestination: String = Screen.Dashboard.route) {
    val navController = rememberNavController()
    var showAddMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val settingsRepository = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
    val allowAutomation by settingsRepository.allowExternalAutomation.collectAsStateWithLifecycle(initialValue = false)
    val items = buildList {
        add(Screen.Dashboard)
        add(Screen.Protect)
        add(Screen.Connect)
        add(Screen.Schedule)
    }
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    androidx.compose.runtime.CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Row(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            NavigationRail(
                modifier = Modifier.width(96.dp).fillMaxHeight(),
                header = {
                    Box(
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 12.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) {
                Spacer(Modifier.height(16.dp))
                items.forEach { screen ->
                    NavigationRailItem(
                        icon = { 
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (currentRoute == screen.route) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    screen.icon, 
                                    contentDescription = screen.title,
                                    tint = if (currentRoute == screen.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        },
                        label = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationRoute ?: "dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
        
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = androidx.compose.ui.graphics.Color.White,
                        actionIconContentColor = androidx.compose.ui.graphics.Color.White,
                        navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    ),
                    title = {
                        Text(
                            if (currentRoute == Screen.Settings.route) "Settings"
                            else if (currentRoute == Screen.FocusCalendar.route) Screen.FocusCalendar.title
                            else items.find { it.route == currentRoute }?.title ?: "Shield",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        if (currentRoute == Screen.Settings.route || currentRoute == Screen.FocusCalendar.route) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        var expandedThemeMenu by remember { mutableStateOf(false) }
                        if (currentRoute != Screen.Settings.route) {
                            IconButton(onClick = { showInfoDialog = currentRoute }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Information")
                            }
                        }
                        if (currentRoute == Screen.Dashboard.route) {
                            IconButton(onClick = { navController.navigate(Screen.Protect.route) }) {
                                Icon(Icons.Default.Contacts, contentDescription = "VIP Contacts")
                            }
                        }
                        if (currentRoute != Screen.Settings.route) {
                            androidx.compose.foundation.layout.Box {
                                IconButton(onClick = { expandedThemeMenu = true }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Palette, contentDescription = "Theme")
                                }
                                androidx.compose.material3.DropdownMenu(expanded = expandedThemeMenu, onDismissRequest = { expandedThemeMenu = false }) {
                                    val coroutineScope = rememberCoroutineScope()
                                    val scope = coroutineScope
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("System Default") }, onClick = { scope.launch { settingsRepository.updateString(SettingsRepository.APP_THEME, "system") }; expandedThemeMenu = false })
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("Light Mode") }, onClick = { scope.launch { settingsRepository.updateString(SettingsRepository.APP_THEME, "light") }; expandedThemeMenu = false })
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("Dark Mode") }, onClick = { scope.launch { settingsRepository.updateString(SettingsRepository.APP_THEME, "dark") }; expandedThemeMenu = false })
                                }
                            }
                            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                )
            },
            snackbarHost = { 
                androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) { data ->
                    androidx.compose.material3.Snackbar(
                        modifier = Modifier.padding(12.dp),
                        containerColor = Color(0xFF10B981), // Premium Vibrant Green
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.White
                            )
                            Text(text = data.visuals.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isExpanded && currentRoute != Screen.Settings.route) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(32.dp)),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            items.forEachIndexed { index, screen ->
                                if (index == 2) {
                                    Spacer(modifier = Modifier.weight(0.5f))
                                }
                                NavigationBarItem(
                                    icon = {
                                         Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = if (currentRoute == screen.route) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                screen.icon,
                                                 contentDescription = screen.title,
                                                tint = if (currentRoute == screen.route) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                         }
                                    },
                                    label = { Text(screen.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationRoute ?: "dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Integrated Geometric FAB
                        FloatingActionButton(
                            onClick = { showAddMenu = true },
                            containerColor = androidx.compose.ui.graphics.Color(0xFF2962FF), // Deep tech blue
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            shape = RoundedCornerShape(16.dp), // Premium geometric shape
                            elevation = FloatingActionButtonDefaults.elevation(8.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-16).dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.weight(1f)
                ) {
                composable(Screen.Dashboard.route) { com.example.ui.screens.DashboardScreen(viewModel, navController) }
                composable(Screen.Protect.route) { com.example.ui.screens.ProtectScreen(viewModel) }
                composable(Screen.Connect.route) { com.example.ui.screens.ConnectScreen(viewModel) }
                composable(Screen.Schedule.route) { com.example.ui.screens.ScheduleScreen(viewModel, onNavigateToAdd = { navController.navigate(Screen.AddSchedule.route) }, onNavigateToFocus = { navController.navigate(Screen.FocusCalendar.route) }) }

                composable(Screen.AddSchedule.route) { 
                    val scheduleViewModel: com.example.ui.viewmodels.ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.ui.viewmodels.ScheduleViewModel.Factory(
                            (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.ShieldApplication).container.scheduledTaskRepository
                        )
                    )
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.example.ui.screens.AddScheduleScreen(
                        onDismiss = { navController.popBackStack() },
                        onSave = { type, target, message, time, isRecurring, intervalMillis ->
                            scheduleViewModel.addTask(type, target, message, time, isRecurring, intervalMillis) { taskId ->
                                val delay = time - System.currentTimeMillis()
                                if (delay > 0) {
                                    val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.shield.ScheduledTaskWorker>()
                                        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                                        .setInputData(androidx.work.Data.Builder().putInt("taskId", taskId.toInt()).build())
                                        .build()
                                    androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
                                }
                            }
                            coroutineScope.launch { snackbarHostState.showSnackbar("Task Scheduled") }
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.FocusCalendar.route) { com.example.ui.screens.ScheduleFocusScreen(navController) }

                composable(Screen.Settings.route) { com.example.ui.screens.SettingsScreen() }
                }
            }
        }
    }

    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            UniversalAddMenuContent(
                onDismiss = { showAddMenu = false },
                navController = navController
            )
        }
    }

    showInfoDialog?.let { route ->
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = {
                Text(
                    when (route) {
                        Screen.Dashboard.route -> "Dashboard & VIPs"
                        Screen.Protect.route -> "Shield & Block"
                        Screen.Connect.route -> "Automate & Connect"
                        Screen.Schedule.route -> "Schedule Tasks"
                        else -> "Information"
                    }
                )
            },
            text = {
                Text(
                    when (route) {
                        Screen.Dashboard.route -> "The Dashboard gives you an overview of app activity. Here, you can also add VIP contacts. VIPs can bypass Do Not Disturb mode so you never miss an emergency. This requires Notification Access to manage DND and Read Call Log/Contacts to identify VIPs."
                        Screen.Protect.route -> "Shield allows you to block specific phone numbers or prefixes entirely. This keeps your phone quiet from known spammers. It requires Call Screening permissions to intercept and reject calls."
                        Screen.Connect.route -> "Connect lets you forward important calls to another phone number. This requires Call Log permissions."
                        Screen.Schedule.route -> "Schedule allows you to set up messages to be sent at a later time. You can pre-plan SMS or WhatsApp messages. This requires SMS permissions to send texts on your behalf."
                        else -> "Features require specific permissions only when you use them."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = null }) {
                    Text("Got it")
                }
            }
        )
    }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UniversalAddMenuContent(onDismiss: () -> Unit, navController: androidx.navigation.NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
    val scope = rememberCoroutineScope()
    
    val ghostMode by settingsRepo.ghostMode.collectAsState(initial = false)
    
    var selectedDuration by remember { mutableStateOf(1) } // in hours
    
    val contactPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                        if (idIndex >= 0) {
                            val id = cursor.getString(idIndex)
                            val values = android.content.ContentValues()
                            values.put(android.provider.ContactsContract.Contacts.STARRED, 1)
                            context.contentResolver.update(
                                android.provider.ContactsContract.Contacts.CONTENT_URI,
                                values,
                                android.provider.ContactsContract.Contacts._ID + " = ?",
                                arrayOf(id)
                            )
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "Contact added to VIPs (Starred)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (ghostMode) {
            Column {
                Text(
                    "Shield is Active",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "All non-VIP calls and messages are being silently blocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = {
                    scope.launch {
                        settingsRepo.updateBoolean(com.example.data.repository.SettingsRepository.GHOST_MODE, false)
                        android.widget.Toast.makeText(context, "Shield Deactivated", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Turn Off Shield", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Column {
                Text(
                    "Activate Shield",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Block all calls and texts. VIPs and emergencies will bypass.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Duration
            Column {
                Text("Duration", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 8).forEach { hours ->
                        val isSelected = selectedDuration == hours
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDuration = hours },
                            label = { Text(if (hours == 8) "Until Morning" else "$hours Hours") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
            
            // Exceptions
            Column {
                Text("Allowed to Bypass", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable { contactPickerLauncher.launch(null) }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Starred Contacts (VIPs)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Tap to add a new VIP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency SMS with 'URGENT' will override.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
    
            Button(
                onClick = {
                    scope.launch {
                        val endTime = System.currentTimeMillis() + (selectedDuration * 60 * 60 * 1000L)
                        settingsRepo.updateBoolean(com.example.data.repository.SettingsRepository.GHOST_MODE, true)
                        settingsRepo.updateLong(com.example.data.repository.SettingsRepository.GHOST_MODE_PAUSE_END_TIME, 0L)
                        android.widget.Toast.makeText(context, "Shield Activated for $selectedDuration hours", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Start Shield", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
