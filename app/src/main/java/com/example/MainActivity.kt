package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CreativeDirectionScreen
import com.example.ui.screens.DirectorChatDialog
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StudioEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PromoViewModel

const val ROUTE_HOME = "home"
const val ROUTE_SCANNER = "scanner"
const val ROUTE_DIRECTION = "direction"
const val ROUTE_STUDIO = "studio"
const val ROUTE_EXPORT = "export"

class MainActivity : ComponentActivity() {

    private val promoViewModel: PromoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PromoVideoApp(viewModel = promoViewModel)
            }
        }
    }
}

@Composable
fun PromoVideoApp(viewModel: PromoViewModel) {
    val navController = rememberNavController()
    var showDirectorChat by remember { mutableStateOf(false) }

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
                onOpenDirectorChat = { showDirectorChat = true }
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
                onNavigateToStudio = { navController.navigate(ROUTE_STUDIO) }
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

    if (showDirectorChat) {
        DirectorChatDialog(
            viewModel = viewModel,
            onDismiss = { showDirectorChat = false }
        )
    }
}
