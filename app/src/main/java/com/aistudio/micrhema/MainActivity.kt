package com.aistudio.micrhema
import android.content.Context
import android.content.Intent
import android.app.Activity
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistudio.micrhema.ui.theme.MICRhemaTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight



fun getIconFromName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when(name) {
        "Home" -> androidx.compose.material.icons.Icons.Default.Home
        "Book" -> androidx.compose.material.icons.Icons.Default.Book
        "Church" -> androidx.compose.material.icons.Icons.Default.Home
        "LibraryBooks" -> androidx.compose.material.icons.Icons.Filled.List
        "Favorite" -> androidx.compose.material.icons.Icons.Default.Favorite
        "People" -> androidx.compose.material.icons.Icons.Default.Person
        "Group" -> androidx.compose.material.icons.Icons.Default.AccountCircle
        "Groups" -> androidx.compose.material.icons.Icons.Default.Group
        "Info" -> androidx.compose.material.icons.Icons.Default.Info
        "Settings" -> androidx.compose.material.icons.Icons.Default.Settings
        "Lock" -> androidx.compose.material.icons.Icons.Default.Lock
        "Video" -> androidx.compose.material.icons.Icons.Default.PlayArrow
        "Photo" -> androidx.compose.material.icons.Icons.Default.Face
        "Link" -> androidx.compose.material.icons.Icons.Default.Share
        else -> androidx.compose.material.icons.Icons.Default.Star
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Devotionals : Screen("devotionals", "Devocionais", Icons.Default.Book)
    object Services : Screen("services", "Cultos", Icons.Default.Church)
    object Prayer : Screen("prayer", "Oração", Icons.Default.Favorite)
    object Members : Screen("members", "Membro (VIP)", Icons.Default.People)
    object Ibr : Screen("ibr", "IBR", Icons.Default.Group)
    object Team : Screen("team", "Equipe", Icons.Default.Group)
    object About : Screen("about", "Sobre", Icons.Default.Info)
    object Settings : Screen("settings", "Configurações", Icons.Default.Settings)
    object Content : Screen("content", "Conteúdo", Icons.Default.LibraryBooks)
    object Admin : Screen("admin", "Área ADM", Icons.Default.Lock)
}

val drawerItems = listOf(
    Screen.Home,
    Screen.Devotionals,
    Screen.Services,
    Screen.Prayer,
    Screen.Members,
    Screen.Ibr,
    Screen.About,
    Screen.Settings,
    Screen.Content,
    Screen.Admin
)



fun performLogout(context: Context, navController: androidx.navigation.NavHostController? = null) {
    userIsAdmin.value = false
    MemberManager.setLoggedInMember(context, null)
    try { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() } catch(e: Exception) {}
    
    // Ensure shared preferences are wiped
    val prefs = context.getSharedPreferences("micrhema_members_prefs", Context.MODE_PRIVATE)
    prefs.edit().remove("logged_in_member_id").apply()
    
    if (navController != null) {
        navController.navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    } else {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        (context as? Activity)?.finishAffinity()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        VersionManager.checkVersion(this)
        
        try {
            currentThemeMode.value = SettingsManager.getThemeMode(this)
            isOfflineModeState.value = SettingsManager.isOfflineMode(this)
            setContent {
                val isDark = when (currentThemeMode.value) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                MICRhemaTheme(darkTheme = isDark) {
                    if (VersionManager.showForceUpdateDialog.value) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { },
                            title = { androidx.compose.material3.Text("Atualização Necessária") },
                            text = { androidx.compose.material3.Text("Uma nova versão do aplicativo está disponível e é necessária para continuar. Por favor, atualize o aplicativo.") },
                            confirmButton = {
                                androidx.compose.material3.Button(onClick = { VersionManager.openUpdateLink(this@MainActivity) }) {
                                    androidx.compose.material3.Text("Atualizar")
                                }
                            },
                            properties = androidx.compose.ui.window.DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false
                            )
                        )
                    }

                    val lastCrash = CrashHandler.getLastCrash(this@MainActivity)
                    if (lastCrash != null) {
                        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                            androidx.compose.material3.Text("CRASH LOG:", color = androidx.compose.ui.graphics.Color.Red)
                            androidx.compose.material3.Button(onClick = { CrashHandler.clearLastCrash(this@MainActivity) }) {
                                androidx.compose.material3.Text("Clear")
                            }
                            androidx.compose.material3.Text(lastCrash)
                        }
                    } else {
                        MainScreen()
                    }
                }
            }
        } catch (e: Exception) {
            setContent {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    androidx.compose.material3.Text("CRASH IN ONCREATE:", color = androidx.compose.ui.graphics.Color.Red)
                    androidx.compose.material3.Text(android.util.Log.getStackTraceString(e))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val visibleTabs = appTabsState.filter { it.isVisible }.sortedBy { it.order }
    val bottomBarItems = visibleTabs.filter { it.showInBottomBar }
    val drawerItems = visibleTabs.filter { !it.showInBottomBar }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled
    }

    LaunchedEffect(Unit) {
        // Initialize Firebase if keys are present (via Secrets panel/BuildConfig)
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty() && com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setProjectId(com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(com.aistudio.micrhema.BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(com.aistudio.micrhema.BuildConfig.FIREBASE_API_KEY)
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(context, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        NotificationHelper.createNotificationChannel(context)
        try {
            NotificationHelper.scheduleDailyReminder(context)
            try {
                NotificationHelper.scheduleDevotionalSync(context)
            } catch(e: Exception) {}
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("devotionals")
            } catch(e: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
        RemoteConfigManager.init()

        LocalDataManager.loadAll(context)
        if (devotionalsState.isEmpty()) {
            loadDevotionalsFromJson(context)
        }
        DevotionalManager.syncDevotionals(context)
        initializeTabs()
        loadContentFromFirebase(context)
        loadTeamMembersFromFirebase()
        MemberManager.syncFromFirestore(context)
        MemberManager.loadMembers(context)
        
        launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                LocalDataManager.saveAll(context)
            }
        }
        
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(context)) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }
    var topBarTitle by remember { mutableStateOf(Screen.Home.title) }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        currentRoute = destination.route ?: Screen.Home.route
        val foundTab = appTabsState.find { (it.systemRoute ?: "custom_tab/${it.id}") == currentRoute }
        topBarTitle = foundTab?.title ?: "MIC Rhema"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                drawerItems.forEach { item ->
                    val route = item.systemRoute ?: "custom_tab/${item.id}"
                    NavigationDrawerItem(
                        icon = { Icon(getIconFromName(item.iconName), contentDescription = null) },
                        label = { Text(item.title) },
                        selected = currentRoute == route,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            if (item.title.equals("Sair", ignoreCase = true) || item.title.equals("Logout", ignoreCase = true)) {
                                scope.launch { drawerState.close() }
                                performLogout(context, navController)
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                                scope.launch { drawerState.close() }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                if (loggedInMemberState.value != null || userIsAdmin.value) {
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    NavigationDrawerItem(
                        icon = { Icon(androidx.compose.material.icons.Icons.Default.ExitToApp, contentDescription = "Sair") },
                        label = { Text("Sair") },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.error,
                            unselectedTextColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            userIsAdmin.value = false
                            MemberManager.setLoggedInMember(context, null)
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (!isCompact) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                ) {
                    bottomBarItems.forEach { item ->
                        val route = item.systemRoute ?: "custom_tab/${item.id}"
                        NavigationRailItem(
                            icon = { Icon(getIconFromName(item.iconName), contentDescription = null) },
                            label = { Text(item.title) },
                            selected = currentRoute == route,
                            onClick = {
                                if (item.title.equals("Sair", ignoreCase = true) || item.title.equals("Logout", ignoreCase = true)) {
                                    performLogout(context, navController)
                                } else {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    GlassTopAppBar(
                        title = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_rhema_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(36.dp).padding(end = 8.dp)
                                )
                                Text(topBarTitle, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            if (isCompact) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    Column(modifier = if (!isCompact) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier) {
                        PersistentAudioPlayerBar()
                        if (isCompact) {
                            GlassNavigationBar {
                                bottomBarItems.forEach { item ->
                                    val route = item.systemRoute ?: "custom_tab/${item.id}"
                                    NavigationBarItem(
                                        icon = { Icon(getIconFromName(item.iconName), contentDescription = null) },
                                        label = { Text(item.title) },
                                        selected = currentRoute == route,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (RemoteConfigManager.showWarningBanner.value && RemoteConfigManager.warningBannerText.value.isNotBlank()) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (RemoteConfigManager.promoLinkUrl.value.isNotBlank()) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RemoteConfigManager.promoLinkUrl.value))
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = RemoteConfigManager.warningBannerText.value,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (RemoteConfigManager.promoLinkUrl.value.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Link",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enterTransition = {
                    slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Devotionals.route) { DevotionalsScreen() }
                composable(Screen.Services.route) { ServicesScreen() }
                composable(Screen.Prayer.route) { PrayerScreen() }
                composable(Screen.Members.route) { MembersScreen() }
                composable(Screen.Ibr.route) { IbrScreen() }
                composable("team") { TeamScreen() }
                composable(Screen.About.route) { AboutScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.Content.route) { ContentScreen() }
                composable(Screen.Admin.route) { AdminScreen() }
                composable("bible") { BibleScreen() }
                composable(
                    route = "custom_tab/{id}",
                    arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val tabId = backStackEntry.arguments?.getString("id")
                    CustomTabScreen(tabId)
                }
            }
            }
        }
        }
        if (GlobalAudioPlayer.isExpanded.value) {
            ExpandedAudioPlayerModal()
        }
    }
}

