package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.build.BuildRequest
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuildPanel(
    buildService: BuildService,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val buildState by buildService.buildState.collectAsState()
    val buildResult by buildService.activeBuildResult.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Build Pipeline Control",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("build_status_card"),
            colors = CardDefaults.cardColors(
                containerColor = when (buildState) {
                    BuildState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                    BuildState.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
                    BuildState.FAILED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Build State: $buildState",
                    style = MaterialTheme.typography.titleSmall,
                    color = when (buildState) {
                        BuildState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                        BuildState.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
                        BuildState.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                val message = buildResult?.message ?: "Pipeline is ready."
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Artifact Info if available
        buildResult?.artifactInfo?.let { artifact ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("build_artifact_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last Generated Artifact",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Path: ${artifact.path}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Variant: ${artifact.variant}", style = MaterialTheme.typography.bodySmall)
                    
                    val date = Date(artifact.timestamp)
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text(text = "Timestamp: ${df.format(date)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Trigger Button
        Button(
            onClick = {
                coroutineScope.launch {
                    val request = BuildRequest(
                        target = "app",
                        variant = "debug",
                        operation = "assembleDebug"
                    )
                    buildService.executeBuild(request)
                }
            },
            enabled = buildState != BuildState.RUNNING,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("build_debug_apk_button")
        ) {
            if (buildState == BuildState.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Building APK...")
            } else {
                Text("Build Debug APK")
            }
        }
    }
}
