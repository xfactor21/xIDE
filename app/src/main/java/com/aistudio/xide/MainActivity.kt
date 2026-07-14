package com.aistudio.xide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.aistudio.xide.core.build.ArtifactResolver
import com.aistudio.xide.core.build.BuildServiceImpl
import com.aistudio.xide.core.build.GradleBuildProvider
import com.aistudio.xide.core.build.ui.BuildScreen
import com.aistudio.xide.core.build.ui.BuildViewModel
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.dashboard.DashboardEvent
import com.aistudio.xide.core.dashboard.DashboardScreen
import com.aistudio.xide.core.dashboard.DashboardState
import com.aistudio.xide.core.designsystem.layout.ResponsiveLayout
import com.aistudio.xide.core.designsystem.theme.XideTheme
import com.aistudio.xide.core.navigation.DestinationRegistry
import com.aistudio.xide.core.shell.ApplicationShell
import com.aistudio.xide.core.shell.NavigationState
import com.aistudio.xide.core.shell.ShellState

// @AndroidEntryPoint // Disabled temporarily due to AGP 9.1.1
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val destinationRegistry = DestinationRegistry()
    
    // Core Build & Diagnostics pipeline setup
    val diagnosticsEngine = DiagnosticsEngineImpl()
    val buildProvider = GradleBuildProvider { "." }
    val artifactResolver = ArtifactResolver { "." }
    val buildService = BuildServiceImpl(listOf(buildProvider), diagnosticsEngine, artifactResolver)
    val buildViewModel = BuildViewModel(buildService, diagnosticsEngine) { "." }
    
    setContent {
      XideTheme {
        var shellState by remember { mutableStateOf(ShellState()) }
        val configuration = LocalConfiguration.current
        val windowSizeClass = ResponsiveLayout.calculateWindowSizeClass(configuration.screenWidthDp)
        
        ApplicationShell(
            shellState = shellState,
            windowSizeClass = windowSizeClass,
            onNavigate = { route ->
                shellState = shellState.copy(
                    navigationState = NavigationState(currentRoute = route)
                )
            }
        ) {
            when (shellState.navigationState.currentRoute) {
                "dashboard" -> DashboardScreen(
                    state = DashboardState(),
                    onEvent = {}
                )
                "templates" -> BuildScreen(
                    viewModel = buildViewModel
                )
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Screen: ${shellState.navigationState.currentRoute} (Not Implemented)")
                    }
                }
            }
        }
      }
    }
  }
}

