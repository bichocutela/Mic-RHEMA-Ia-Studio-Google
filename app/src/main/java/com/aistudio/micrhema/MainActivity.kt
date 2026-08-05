package com.aistudio.micrhema
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.VolunteerActivism
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
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.unit.dp
import androidx.navigation.navArgument
import android.app.Activity
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
        "ConfirmationNumber" -> androidx.compose.material.icons.Icons.Default.ConfirmationNumber
        "DateRange" -> androidx.compose.material.icons.Icons.Default.DateRange
        else -> androidx.compose.material.icons.Icons.Default.Star
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Devotionals : Screen("devocionais", "Devocionais", Icons.Default.Book)
    object Services : Screen("services", "Cultos", Icons.Default.Church)
    object Prayer : Screen("prayer", "Oração", Icons.Default.Favorite)
    object Members : Screen("members", "Membro (VIP)", Icons.Default.People)
    object Ibr : Screen("ibr", "IBR", Icons.Default.Group)
    object Plans : Screen("plans", "Planos", Icons.Default.List)
    object Team : Screen("equipe", "Equipe", Icons.Default.Group)
    object About : Screen("about", "Sobre", Icons.Default.Info)
    object Donations : Screen("donations", "Dízimos e Ofertas", Icons.Default.VolunteerActivism)
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
    Screen.Donations,
    Screen.Settings,
    Screen.Content,
    Screen.Admin
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            currentThemeMode.value = SettingsManager.getThemeMode(this)
            isOfflineModeState.value = SettingsManager.isOfflineMode(this)
            UserSettingsManager.loadSettings(this)
            setContent {
                val isDark = when (currentThemeMode.value) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                MICRhemaTheme(darkTheme = isDark) {
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


    LaunchedEffect(loggedInMemberState.value) {
        if (loggedInMemberState.value != null) {
            loadFavoritesFromFirestore()
        } else {
            favoriteItemsState.clear()
        }
    }

    LaunchedEffect(Unit) {
        // Enforce cleanup on the UI side directly
        val toDelete = appTabsState.filter { it.title.contains("Ajuda", ignoreCase = true) || it.title.contains("Eventos", ignoreCase = true) || it.id == "9" || it.id == "eventos_tab" || it.id == "ajuda_tab" || it.id == "events_tab" || it.id == "help_tab" }
        appTabsState.removeAll(toDelete)
        
        val sobreIdx = appTabsState.indexOfFirst { it.id == "8" }
        if (sobreIdx != -1) {
            appTabsState[sobreIdx] = appTabsState[sobreIdx].copy(isVisible = true, title = "Sobre", iconName = "Info", systemRoute = Screen.About.route)
        } else {
            appTabsState.add(AppTab("8", "Sobre", "Info", false, true, false, 9, TabContentType.SYSTEM, Screen.About.route))
        }
        
        val adminIdx = appTabsState.indexOfFirst { it.id == "admin_tab" }
        if (adminIdx != -1) {
            appTabsState[adminIdx] = appTabsState[adminIdx].copy(isVisible = true)
        } else {
            appTabsState.add(AppTab("admin_tab", "Área ADM", "Lock", false, true, false, 11, TabContentType.SYSTEM, Screen.Admin.route))
        }

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
                NotificationHelper.scheduleServiceAlert(context)
            } catch(e: Exception) {}
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("devocionais")
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
        loadBannersFromFirestore()
        loadDonationsFromFirestore()
        syncBibleNewsAndPlans()
        MemberManager.loadMembers(context)
        MemberManager.syncFromFirestore(context)
        
        // Force refresh from Firestore on app open
        kotlinx.coroutines.GlobalScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork().await()
            } catch(e: Exception) {}
        }
        
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
        val foundTab = appTabsState.find { (if (it.id == "bible_tab") "bible" else (it.systemRoute ?: "custom_tab/${it.id}")) == currentRoute }
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
                    val route = if (item.id == "bible_tab") "bible" else (item.systemRoute ?: "custom_tab/${item.id}")
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
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
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
                        val route = if (item.id == "bible_tab") "bible" else (item.systemRoute ?: "custom_tab/${item.id}")
                        NavigationRailItem(
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

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                        PersistentAudioPlayerBar()
                        if (isCompact) {
                            FloatingNavigationBar(
                                items = bottomBarItems,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                },
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }
            ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues).fillMaxSize()) {
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
                composable(Screen.Home.route) { HomeScreen(onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }) }
                composable(Screen.Devotionals.route) { DevotionalsScreen() }
                composable("devotionals") { DevotionalsScreen() }
                composable(Screen.Services.route) { ServicesScreen() }
                composable(Screen.Prayer.route) { PrayerScreen() }
                composable(Screen.Members.route) { MembersScreen() }
                composable(Screen.Ibr.route) { 
                    IbrMainScreen(
                        onNavigateToCourse = { courseId -> navController.navigate("ibr_course/$courseId") }
                    ) 
                }
                
                composable("ibr_course/{courseId}") { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
                    IbrCourseScreen(
                        courseId = courseId,
                        onBack = { navController.popBackStack() },
                        onNavigateToText = { cid, chid -> navController.navigate("ibr_text/$cid/$chid") }
                    )
                }
                
                composable("ibr_text/{courseId}/{chapterId}") { backStackEntry ->
                    val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
                    val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                    IbrTextScreen(
                        courseId = courseId,
                        chapterId = chapterId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("equipe") { TeamScreen() }
                composable("team") { TeamScreen() }
                composable("plans") { PlansScreen(onNavigateToBible = { book, chap -> navController.navigate("bible?book=$book&chapter=$chap") }) }
                composable(Screen.About.route) { AboutScreen() }
                composable(Screen.Donations.route) { DonationsScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.Content.route) { ContentScreen() }
                composable(Screen.Admin.route) { AdminScreen() }
                composable("news_list") { NewsListScreen(
                    onNavigateToDetail = { id -> navController.navigate("news_detail/$id") },
                    onBack = { navController.popBackStack() }
                ) }
                composable(
                    route = "news_detail/{id}",
                    arguments = listOf(navArgument("id") { type = androidx.navigation.NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                    NewsDetailScreen(
                        newsId = id,
                        onBack = { navController.popBackStack() },
                        onNavigateToBible = { book, chap, version -> navController.navigate("bible?book=$book&chapter=$chap&version=$version") }
                    )
                }
                composable(
                    route = "bible?book={book}&chapter={chapter}&version={version}",
                    arguments = listOf(
                        navArgument("book") { nullable = true; defaultValue = null },
                        navArgument("chapter") { nullable = true; defaultValue = null },
                        navArgument("version") { nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val book = backStackEntry.arguments?.getString("book")
                    val chapter = backStackEntry.arguments?.getString("chapter")?.toIntOrNull()
                    val version = backStackEntry.arguments?.getString("version")?.takeIf { it.isNotBlank() }
                    BibleScreen(initialBook = book, initialChapter = chapter, initialVersion = version)
                }
                composable(
                    route = "custom_tab/{id}",
                    arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val tabId = backStackEntry.arguments?.getString("id")
                    CustomTabScreen(tabId)
                }
            }
        }
        
        if (GlobalAudioPlayer.isExpanded.value) {
            ExpandedAudioPlayerModal()
        }
        }
        }
    }
}


