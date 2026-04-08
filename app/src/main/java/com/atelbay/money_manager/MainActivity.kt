package com.atelbay.money_manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.components.CircleRevealShape
import com.atelbay.money_manager.core.ui.theme.LocalStrings
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.appStringsFor
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
import com.atelbay.money_manager.navigation.Home
import com.atelbay.money_manager.navigation.Import
import com.atelbay.money_manager.navigation.MoneyManagerBottomBar
import com.atelbay.money_manager.navigation.MoneyManagerBottomBarDefaults
import com.atelbay.money_manager.navigation.MoneyManagerNavHost
import com.atelbay.money_manager.navigation.NavigationAction
import com.atelbay.money_manager.navigation.Onboarding
import com.atelbay.money_manager.navigation.PendingNavigationManager
import com.atelbay.money_manager.navigation.TopLevelDestination
import com.atelbay.money_manager.navigation.TransactionEdit
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.sqrt
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var pendingNavigationManager: PendingNavigationManager

    @Volatile
    private var onboardingLoadCompleted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !onboardingLoadCompleted }

        extractPdfUri(intent)?.let {
            pendingNavigationManager.enqueue(NavigationAction.OpenImport(it))
            pendingNavigationManager.markExternalLaunch()
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")

            val languageCode by userPreferences.languageCode
                .collectAsStateWithLifecycle(initialValue = "ru")

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                )
                onDispose {}
            }

            val onboardingCompleted by userPreferences.isOnboardingCompleted
                .collectAsStateWithLifecycle(initialValue = null)

            SideEffect {
                if (onboardingCompleted != null) {
                    onboardingLoadCompleted = true
                }
            }

            val completed = onboardingCompleted

            CompositionLocalProvider(LocalStrings provides appStringsFor(languageCode)) {
                if (completed != null) {
                    val navController = rememberNavController()
                    val snapshotLayer = rememberGraphicsLayer()
                    val reduceMotion = LocalReduceMotion.current

                    var renderedTheme by remember { mutableStateOf(themeMode) }
                    val revealRadius = remember { Animatable(0f) }
                    var isRevealing by remember { mutableStateOf(false) }
                    var skipInitial by remember { mutableStateOf(true) }

                    LaunchedEffect(themeMode) {
                        if (skipInitial) {
                            skipInitial = false
                            renderedTheme = themeMode
                            return@LaunchedEffect
                        }
                        if (themeMode != renderedTheme) {
                            if (reduceMotion) {
                                renderedTheme = themeMode
                                return@LaunchedEffect
                            }
                            val dm = resources.displayMetrics
                            val screenW = resources.configuration.screenWidthDp * dm.density
                            val screenH = resources.configuration.screenHeightDp * dm.density
                            val maxRadius = sqrt(screenW * screenW + screenH * screenH)
                            isRevealing = true
                            revealRadius.snapTo(maxRadius)
                            renderedTheme = themeMode
                            revealRadius.animateTo(
                                0f,
                                tween(
                                    MoneyManagerMotion.DurationLong,
                                    easing = MoneyManagerMotion.StandardEasing,
                                ),
                            )
                            isRevealing = false
                        }
                    }

                    Box {
                        MoneyManagerTheme(themeMode = themeMode) {
                            Box(
                                modifier = Modifier.drawWithContent {
                                    snapshotLayer.record(
                                        IntSize(size.width.toInt(), size.height.toInt()),
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                    drawContent()
                                },
                            ) {
                                MoneyManagerApp(
                                    navController = navController,
                                    startDestination = if (completed) Home else Onboarding,
                                    onboardingCompleted = completed,
                                    pendingNavigationManager = pendingNavigationManager,
                                )
                            }
                        }
                        if (isRevealing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        CircleRevealShape(
                                            radius = revealRadius.value,
                                            inverted = true,
                                        ),
                                    )
                                    .drawWithContent {
                                        drawLayer(snapshotLayer)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractPdfUri(intent)?.let {
            pendingNavigationManager.enqueue(NavigationAction.OpenImport(it))
            pendingNavigationManager.markExternalLaunch()
        }
    }

    private fun extractPdfUri(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_SEND -> {
            // Manifest filter already guarantees mimeType=application/pdf;
            // fall back to ClipData for apps that don't set EXTRA_STREAM
            @Suppress("DEPRECATION")
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            val clipUri = intent.clipData?.getItemAt(0)?.uri
            (streamUri ?: clipUri)?.also { takePersistablePermission(it) }?.toString()
        }
        Intent.ACTION_VIEW -> {
            // Manifest filter already guarantees mimeType=application/pdf;
            // intent.type is often null for ACTION_VIEW — trust the filter, not the field
            intent.data?.also { takePersistablePermission(it) }?.toString()
        }
        else -> null
    }

    private fun takePersistablePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // file:// URIs don't support persistable permissions — safe to ignore
        }
    }
}

@Composable
private fun MoneyManagerApp(
    navController: NavHostController,
    startDestination: Any,
    onboardingCompleted: Boolean,
    pendingNavigationManager: PendingNavigationManager,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTopLevel = TopLevelDestination.entries.find { dest ->
        currentDestination?.hasRoute(dest.route::class) == true
    }

    val showBottomBar = currentTopLevel != null

    // Observe pending action as Compose state so this LaunchedEffect re-runs when
    // either the back stack settles (backStackEntry becomes non-null) or a new action arrives.
    val pendingAction by pendingNavigationManager.pendingAction.collectAsStateWithLifecycle()
    LaunchedEffect(backStackEntry?.id, pendingAction) {
        val action = pendingAction ?: return@LaunchedEffect
        if (!onboardingCompleted) return@LaunchedEffect
        backStackEntry ?: return@LaunchedEffect // NavHost not yet settled
        when (action) {
            is NavigationAction.OpenImport -> {
                navController.navigate(Import(pdfUri = action.pdfUri)) {
                    launchSingleTop = true
                }
                pendingNavigationManager.consume()
            }
        }
    }

    // When the app was launched externally (via PDF share/view) and the import screen
    // has been closed (no Import destination in back stack), finish the activity so the
    // user returns to whatever app they shared from.
    val launchedFromExternal by pendingNavigationManager.launchedFromExternal.collectAsStateWithLifecycle()
    val activity = androidx.activity.compose.LocalActivity.current
    LaunchedEffect(backStackEntry?.id, launchedFromExternal) {
        if (!launchedFromExternal) return@LaunchedEffect
        // Wait until the pending navigation action is consumed (Import was navigated to)
        // before checking whether we've left the Import screen.
        if (pendingAction != null) return@LaunchedEffect
        val isOnImportScreen = navController.currentBackStackEntry
            ?.destination?.hasRoute(Import::class) == true
        if (!isOnImportScreen) {
            pendingNavigationManager.clearExternalLaunch()
            activity?.finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MoneyManagerNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            bottomBarPadding = MoneyManagerBottomBarDefaults.Height,
            onFabNavigate = { navController.navigate(TransactionEdit()) },
        )
        if (showBottomBar) {
            MoneyManagerBottomBar(
                currentDestination = currentTopLevel,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
