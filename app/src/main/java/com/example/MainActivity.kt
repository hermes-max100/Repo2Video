package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.authStateFlow
import com.example.service.configureAppCheck
import com.example.ui.screens.AppearanceDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreativeDirectionScreen
import com.example.ui.screens.DirectorChatDialog
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.GoogleOAuthDialog
import com.example.ui.screens.GrokOAuthDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StudioEditorScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PromoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

const val ROUTE_HOME = "home"
const val ROUTE_SCANNER = "scanner"
const val ROUTE_DIRECTION = "direction"
const val ROUTE_STUDIO = "studio"
const val ROUTE_EXPORT = "export"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent BEFORE UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        MyApplicationTheme(themeMode = AppThemeMode.STUDIO_DARK) {
            AuthScreen(onAuthSuccess = { /* AuthStateListener emits authenticated user automatically */ })
        }
    } else {
        val uid = user.uid
        // Key composition and ViewModel by uid so signing out or switching accounts
        // tears down and recreates Firestore listeners cleanly.
        key(uid) {
            val appContext = LocalContext.current.applicationContext as Application
            val promoViewModel: PromoViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] ?: appContext
                        PromoViewModel(app)
                    }
                }
            )

            LaunchedEffect(uid) {
                promoViewModel.bindAuthenticatedUser(
                    userId = uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString()
                )
            }

            val themeMode by promoViewModel.currentThemeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                DevDirectorApp(viewModel = promoViewModel)
            }
        }
    }
}

@Composable
fun DevDirectorApp(viewModel: PromoViewModel) {
    val navController = rememberNavController()
    var showDirectorChat by remember { mutableStateOf(false) }
    var showGrokOAuthDialog by remember { mutableStateOf(false) }
    var showGoogleOAuthDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToScanner = { navController.navigate(ROUTE_SCANNER) },
                onNavigateToStudio = { navController.navigate(ROUTE_STUDIO) },
                onNavigateToExport = { navController.navigate(ROUTE_EXPORT) },
                onOpenDirectorChat = { showDirectorChat = true },
                onOpenGrokOAuth = { showGrokOAuthDialog = true },
                onOpenGoogleOAuth = { showGoogleOAuthDialog = true },
                onOpenAppearance = { showAppearanceDialog = true }
            )
        }

        composable(ROUTE_SCANNER) {
            ScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreativeDirection = { navController.navigate(ROUTE_DIRECTION) }
            )
        }

        composable(ROUTE_DIRECTION) {
            CreativeDirectionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudio = { navController.navigate(ROUTE_STUDIO) },
                onOpenGrokOAuth = { showGrokOAuthDialog = true }
            )
        }

        composable(ROUTE_STUDIO) {
            StudioEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { navController.navigate(ROUTE_EXPORT) },
                onOpenDirectorChat = { showDirectorChat = true }
            )
        }

        composable(ROUTE_EXPORT) {
            ExportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    if (showAppearanceDialog) {
        AppearanceDialog(
            viewModel = viewModel,
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showDirectorChat) {
        DirectorChatDialog(
            viewModel = viewModel,
            onDismiss = { showDirectorChat = false },
            onOpenGrokOAuth = { showGrokOAuthDialog = true }
        )
    }

    if (showGrokOAuthDialog) {
        GrokOAuthDialog(
            viewModel = viewModel,
            onDismiss = { showGrokOAuthDialog = false }
        )
    }

    if (showGoogleOAuthDialog) {
        GoogleOAuthDialog(
            viewModel = viewModel,
            onDismiss = { showGoogleOAuthDialog = false }
        )
    }
}

/**
 * Backward compatibility alias for tests and previews
 */
@Composable
fun PromoVideoApp(viewModel: PromoViewModel) {
    DevDirectorApp(viewModel = viewModel)
}
