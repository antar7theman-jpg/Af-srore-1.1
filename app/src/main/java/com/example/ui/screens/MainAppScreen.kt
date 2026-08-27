package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.ui.theme.AppThemePalette
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.AppLanguage
import com.example.utils.AppStrings

enum class MainTab(val icon: ImageVector) {
    POS(Icons.Default.PointOfSale),
    CUSTOMERS(Icons.Default.People),
    INVENTORY(Icons.Default.Inventory2),
    REPORTS(Icons.Default.Dashboard),
    SETTINGS(Icons.Default.Settings);

    fun getTitle(lang: AppLanguage): String = when (this) {
        POS -> AppStrings.tabPos(lang)
        CUSTOMERS -> AppStrings.tabCustomers(lang)
        INVENTORY -> AppStrings.tabInventory(lang)
        REPORTS -> AppStrings.tabReports(lang)
        SETTINGS -> AppStrings.tabSettings(lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: AntarSalesViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.POS) }
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentThemePalette by viewModel.currentThemePalette.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Reactively show snackbar when message emitted
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // ===== Tablet / Expanded Screen Layout with NavigationRail =====
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("main_navigation_rail")
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 3.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "AF",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "عنتر فوس",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MainTab.values().forEach { tab ->
                                val tabTitle = tab.getTitle(currentLanguage)
                                val isSelected = selectedTab == tab

                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (tab == MainTab.POS && totalCartCount > 0) {
                                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                        Text("$totalCartCount")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tabTitle)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }

                        // Bottom Actions on Navigation Rail (Theme toggle, User, Logout)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            IconButton(
                                onClick = { onToggleDarkTheme(!isDarkTheme) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "تبديل المظهر",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { showLogoutConfirmDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("logout_button"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = AppStrings.logout(currentLanguage),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Main Content for Tablets / Wide Screens
                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = AppStrings.appName(currentLanguage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = selectedTab.getTitle(currentLanguage),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                currentUser?.let { user ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = user.username,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                (slideInHorizontally { width -> (width * 0.2f).toInt() } + fadeIn(animationSpec = tween(200)))
                                    .togetherWith(slideOutHorizontally { width -> (-width * 0.2f).toInt() } + fadeOut(animationSpec = tween(150)))
                            } else {
                                (slideInHorizontally { width -> (-width * 0.2f).toInt() } + fadeIn(animationSpec = tween(200)))
                                    .togetherWith(slideOutHorizontally { width -> (width * 0.2f).toInt() } + fadeOut(animationSpec = tween(150)))
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "tablet_tab_content_transition"
                    ) { tab ->
                        when (tab) {
                            MainTab.POS -> PosScreen(viewModel = viewModel)
                            MainTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                            MainTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                            MainTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                            MainTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme
                            )
                        }
                    }
                }
            }
        } else {
            // ===== Compact Phone Layout with Bottom NavigationBar =====
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Brand Logo Box
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "AF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = AppStrings.appName(currentLanguage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = selectedTab.getTitle(currentLanguage),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Logout Button
                            FilledTonalIconButton(
                                onClick = { showLogoutConfirmDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .padding(end = 4.dp)
                                    .testTag("logout_button"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = AppStrings.logout(currentLanguage),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_navigation_bar")
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        tonalElevation = 2.dp
                    ) {
                        MainTab.values().forEach { tab ->
                            val tabTitle = tab.getTitle(currentLanguage)
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == MainTab.POS && totalCartCount > 0) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text("$totalCartCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tabTitle)
                                    }
                                },
                                label = { Text(tabTitle, fontSize = 11.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> (width * 0.35f).toInt() } + fadeIn(animationSpec = tween(250)))
                                .togetherWith(slideOutHorizontally { width -> (-width * 0.35f).toInt() } + fadeOut(animationSpec = tween(200)))
                        } else {
                            (slideInHorizontally { width -> (-width * 0.35f).toInt() } + fadeIn(animationSpec = tween(250)))
                                .togetherWith(slideOutHorizontally { width -> (width * 0.35f).toInt() } + fadeOut(animationSpec = tween(200)))
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    label = "tab_content_transition"
                ) { tab ->
                    when (tab) {
                        MainTab.POS -> PosScreen(viewModel = viewModel)
                        MainTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                        MainTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                        MainTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                        MainTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme
                        )
                    }
                }
            }
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "Déconnexion"
                    else if (currentLanguage == AppLanguage.ENGLISH) "Log Out"
                    else "تسجيل الخروج",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "Êtes-vous sûr de vouloir vous déconnecter de votre compte ?"
                    else if (currentLanguage == AppLanguage.ENGLISH) "Are you sure you want to log out of your account?"
                    else "هل أنت متأكد من رغبتك في تسجيل الخروج من الحساب الحالي؟",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Se déconnecter"
                        else if (currentLanguage == AppLanguage.ENGLISH) "Log Out"
                        else "نعم، خروج"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(AppStrings.cancel(currentLanguage))
                }
            }
        )
    }
}
