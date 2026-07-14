package com.aistudio.xide.core.build.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.build.BuildState
import com.aistudio.xide.core.designsystem.components.IndicatorStatus
import com.aistudio.xide.core.designsystem.components.XideButton
import com.aistudio.xide.core.designsystem.components.XideCard
import com.aistudio.xide.core.designsystem.components.XidePanel
import com.aistudio.xide.core.designsystem.components.XideStatusIndicator
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens
import com.aistudio.xide.core.designsystem.tokens.TypographyTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BuildScreen(
    viewModel: BuildViewModel,
    modifier: Modifier = Modifier
) {
    val buildState by viewModel.buildState.collectAsState()
    val buildResult by viewModel.activeBuildResult.collectAsState()
    val diagnostics by viewModel.activeDiagnostics.collectAsState()

    XidePanel(modifier = modifier.fillMaxSize().padding(SpacingTokens.Medium).testTag("build_panel")) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.Large)
        ) {
            Text(
                text = "APK Compiler Dashboard",
                style = TypographyTokens.DisplayLarge,
                modifier = Modifier.testTag("build_title")
            )

            // Current Status Panel
            XideCard(modifier = Modifier.fillMaxWidth().testTag("status_panel")) {
                Text(text = "Compilation State", style = TypographyTokens.TitleMedium)
                Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val indicatorStatus = when (buildState) {
                        BuildState.IDLE -> IndicatorStatus.IDLE
                        BuildState.RUNNING -> IndicatorStatus.BUSY
                        BuildState.SUCCESS -> IndicatorStatus.SUCCESS
                        BuildState.FAILED -> IndicatorStatus.ERROR
                        BuildState.CANCELLED -> IndicatorStatus.WARNING
                    }
                    val statusText = when (buildState) {
                        BuildState.IDLE -> "System Idle - Awaiting Task"
                        BuildState.RUNNING -> "Executing Local Gradle Build Process..."
                        BuildState.SUCCESS -> "Build Completed Succeeded"
                        BuildState.FAILED -> "Build Finished with Errors"
                        BuildState.CANCELLED -> "Build Execution Cancelled"
                    }

                    XideStatusIndicator(
                        status = indicatorStatus,
                        message = statusText,
                        modifier = Modifier.testTag("build_status_indicator")
                    )

                    if (buildState == BuildState.RUNNING) {
                        Text(
                            text = "Processing...",
                            style = TypographyTokens.LabelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Build Actions Card
            XideCard(modifier = Modifier.fillMaxWidth().testTag("actions_panel")) {
                Text(text = "Available Target Operations", style = TypographyTokens.TitleMedium)
                Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Medium)
                ) {
                    XideButton(
                        text = "Build Debug APK",
                        onClick = { viewModel.buildDebugApk() },
                        enabled = buildState != BuildState.RUNNING,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("build_debug_btn")
                    )

                    XideButton(
                        text = "Build Release APK",
                        onClick = { viewModel.buildReleaseApk() },
                        enabled = buildState != BuildState.RUNNING,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("build_release_btn")
                    )
                }
            }

            // Artifact Discovery Metadata (Shows on SUCCESS)
            if (buildState == BuildState.SUCCESS && buildResult?.artifactInfo != null) {
                val artifact = buildResult?.artifactInfo!!
                XideCard(modifier = Modifier.fillMaxWidth().testTag("artifact_panel")) {
                    Text(text = "Verified Output Artifact Information", style = TypographyTokens.TitleMedium)
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                    val bytes = artifact.metadata["size_bytes"]?.toLongOrNull() ?: 0L
                    val sizeFormatted = "%.2f MB".format(bytes / (1024.0 * 1024.0))
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(artifact.timestamp))

                    Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.Small)) {
                        Text(text = "Filename: ${artifact.metadata["filename"] ?: "app-debug.apk"}", style = TypographyTokens.BodyMedium)
                        Text(text = "Path: ${artifact.path}", style = TypographyTokens.BodyMedium)
                        Text(text = "Size: $sizeFormatted ($bytes bytes)", style = TypographyTokens.BodyMedium)
                        Text(text = "Timestamp: $dateFormatted", style = TypographyTokens.BodyMedium)
                        Text(text = "Variant Type: ${artifact.variant.uppercase()}", style = TypographyTokens.BodyMedium)

                        Spacer(modifier = Modifier.height(SpacingTokens.Small))

                        XideButton(
                            text = "Verify Artifact Integrity",
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_artifact_btn")
                        )
                    }
                }
            }

            // Diagnostics & Parsing Logs (Shows on FAILED/WARNING or if Diagnostics exist)
            if (diagnostics.isNotEmpty() || buildState == BuildState.FAILED) {
                XideCard(modifier = Modifier.fillMaxWidth().weight(1f).testTag("diagnostics_panel")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Compiler & Parser Diagnostics", style = TypographyTokens.TitleMedium)
                        XideButton(
                            text = "Clear Logs",
                            onClick = { viewModel.clearDiagnostics() },
                            modifier = Modifier.height(36.dp).testTag("clear_diagnostics_btn")
                        )
                    }
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))

                    if (diagnostics.isEmpty()) {
                        Text(
                            text = "No compiler errors or linter warnings captured.",
                            style = TypographyTokens.BodyMedium,
                            modifier = Modifier.padding(SpacingTokens.Medium)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("diagnostics_list"),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.Small)
                        ) {
                            items(diagnostics) { diag ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = SpacingTokens.Small)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "[${diag.severity}] Category: ${diag.category}",
                                            style = TypographyTokens.LabelSmall,
                                            color = if (diag.severity.name == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        val loc = diag.compilerDiagnostic?.location
                                        if (loc != null) {
                                            Text(
                                                text = "Line: ${loc.line ?: "?"}, Col: ${loc.column ?: "?"}",
                                                style = TypographyTokens.LabelSmall
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                                    Text(text = diag.message, style = TypographyTokens.BodyMedium)
                                    val locPath = diag.compilerDiagnostic?.location?.filePath
                                    if (locPath != null) {
                                        Text(
                                            text = "In file: $locPath",
                                            style = TypographyTokens.LabelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(SpacingTokens.Small))
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
