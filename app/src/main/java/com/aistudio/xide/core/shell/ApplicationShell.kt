package com.aistudio.xide.core.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.designsystem.components.IndicatorStatus
import com.aistudio.xide.core.designsystem.components.NavigationItem
import com.aistudio.xide.core.designsystem.components.XideNavigation
import com.aistudio.xide.core.designsystem.components.XideStatusIndicator
import com.aistudio.xide.core.designsystem.components.XideTopBar
import com.aistudio.xide.core.designsystem.layout.WindowSizeClass
import com.aistudio.xide.core.xero.XeroState

@Composable
fun ApplicationShell(
    shellState: ShellState,
    windowSizeClass: WindowSizeClass,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val navItems = listOf(
        NavigationItem("dashboard", Icons.Default.Home, "Home"),
        NavigationItem("projects", Icons.Default.Folder, "Projects"),
        NavigationItem("templates", Icons.Default.Build, "Templates"),
        NavigationItem("settings", Icons.Default.Settings, "Settings"),
        NavigationItem("xero", Icons.Default.Face, "Xero")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            XideTopBar(
                title = "xIDE",
                actions = {
                    XideStatusIndicator(
                        status = if (shellState.statusState.isBusy) IndicatorStatus.BUSY else IndicatorStatus.IDLE,
                        message = shellState.statusState.statusMessage
                    )
                }
            )
        },
        bottomBar = {
            if (windowSizeClass == WindowSizeClass.COMPACT) {
                XideNavigation(
                    items = navItems,
                    currentRoute = shellState.navigationState.currentRoute,
                    onNavigate = onNavigate,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (windowSizeClass != WindowSizeClass.COMPACT) {
                XideNavigation(
                    items = navItems,
                    currentRoute = shellState.navigationState.currentRoute,
                    onNavigate = onNavigate,
                    windowSizeClass = windowSizeClass
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content()
                }

                // Status Bar / Xero Assistant Status Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    XideStatusIndicator(
                        status = when (shellState.xeroState) {
                            XeroState.Idle -> IndicatorStatus.IDLE
                            XeroState.Thinking, XeroState.Working -> IndicatorStatus.BUSY
                            XeroState.Success -> IndicatorStatus.SUCCESS
                            XeroState.Warning -> IndicatorStatus.WARNING
                            XeroState.Error -> IndicatorStatus.ERROR
                            else -> IndicatorStatus.IDLE
                        },
                        message = "Xero: ${shellState.xeroState.name}"
                    )
                }
            }
        }
    }
}
