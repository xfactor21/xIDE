package com.aistudio.xide.core.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aistudio.xide.core.designsystem.components.IndicatorStatus
import com.aistudio.xide.core.designsystem.components.XideButton
import com.aistudio.xide.core.designsystem.components.XideCard
import com.aistudio.xide.core.designsystem.components.XidePanel
import com.aistudio.xide.core.designsystem.components.XideStatusIndicator
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens
import com.aistudio.xide.core.designsystem.tokens.TypographyTokens

@Composable
fun DashboardScreen(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    XidePanel(modifier = modifier.fillMaxSize().padding(SpacingTokens.Medium)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.Large)
        ) {
            Text(
                text = "Welcome to xIDE",
                style = TypographyTokens.DisplayLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Medium)
            ) {
                XideCard(modifier = Modifier.weight(1f)) {
                    Text(text = "Quick Actions", style = TypographyTokens.TitleMedium)
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))
                    XideButton(
                        text = "New Project",
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                    XideButton(
                        text = "Open Project",
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                XideCard(modifier = Modifier.weight(1f)) {
                    Text(text = "Recent Projects", style = TypographyTokens.TitleMedium)
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))
                    if (state.recentProjects.isEmpty()) {
                        Text(text = "No recent projects.", style = TypographyTokens.BodyMedium)
                    } else {
                        state.recentProjects.forEach { project ->
                            Text(text = project, style = TypographyTokens.BodyMedium)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Medium)
            ) {
                XideCard(modifier = Modifier.weight(1f)) {
                    Text(text = "Templates", style = TypographyTokens.TitleMedium)
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))
                    Text(text = "Android App", style = TypographyTokens.BodyMedium)
                    Text(text = "KMP Library", style = TypographyTokens.BodyMedium)
                    Text(text = "Compose Multiplatform", style = TypographyTokens.BodyMedium)
                }

                XideCard(modifier = Modifier.weight(1f)) {
                    Text(text = "System Status", style = TypographyTokens.TitleMedium)
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))
                    XideStatusIndicator(status = IndicatorStatus.SUCCESS, message = "Build System Ready")
                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                    XideStatusIndicator(status = IndicatorStatus.IDLE, message = "No active tasks")
                }
            }
        }
    }
}
