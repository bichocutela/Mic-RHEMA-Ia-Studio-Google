package com.aistudio.micrhema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistudio.micrhema.ui.theme.MICRhemaTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Devotionals : Screen("devotionals", "Devocionais", Icons.Default.Book)
    object Services : Screen("services", "Cultos", Icons.Default.Church)
    object Prayer : Screen("prayer", "Oração", Icons.Default.Favorite)
    object Members : Screen("members", "Membro (VIP)", Icons.Default.People)
    object Ibr : Screen("ibr", "IBR", Icons.Default.Group)
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAppDarkTheme.value = ThemeManager.isDarkTheme(this)
        setContent {
            MICRhemaTheme(darkTheme = isAppDarkTheme.value) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
        try {
            NotificationHelper.scheduleDailyReminder(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadDevotionalsFromJson(context)
        initializeMockContent()
        
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
        topBarTitle = drawerItems.find { it.route == currentRoute }?.title ?: "MIC Rhema"
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
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(item.route) {
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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.rhema_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(36.dp).padding(end = 8.dp)
                            )
                            Text(topBarTitle, color = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    ) + fadeOut(animationSpec = tween(400))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    ) + fadeOut(animationSpec = tween(400))
                }
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Devotionals.route) { DevotionalsScreen() }
                composable(Screen.Services.route) { ServicesScreen() }
                composable(Screen.Prayer.route) { PrayerScreen() }
                composable(Screen.Members.route) { MembersScreen() }
                composable(Screen.Ibr.route) { IbrScreen() }
                composable(Screen.About.route) { AboutScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.Content.route) { ContentScreen() }
                composable(Screen.Admin.route) { AdminScreen() }
            }
        }
    }
}

