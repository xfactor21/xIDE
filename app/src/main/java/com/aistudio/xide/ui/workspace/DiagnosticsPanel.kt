package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine

@Composable
fun DiagnosticsPanel(
    diagnosticsEngine: DiagnosticsEngine,
    onDiagnosticClick: (filePath: String, line: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val diagnostics by diagnosticsEngine.activeDiagnostics.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Text(
            text = "Diagnostics & Build Output",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (diagnostics.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("diagnostics_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No problems found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("diagnostics_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(diagnostics) { diag ->
                    val compilerDiag = diag.compilerDiagnostic
                    val location = compilerDiag?.location
                    val filePath = location?.filePath
                    val line = location?.line
                    val column = location?.column

                    val icon = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> Icons.Default.Error
                        DiagnosticSeverity.WARNING -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    }

                    val iconColor = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                        DiagnosticSeverity.WARNING -> Color(0xFFEAA100) // Beautiful Material Amber/Warning
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (filePath != null && line != null) {
                                    onDiagnosticClick(filePath, line)
                                }
                            }
                            .testTag("diagnostic_card_${diag.category}_${diag.severity}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = diag.severity.name,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = diag.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (filePath != null) {
                                    Text(
                                        text = "$filePath:${line ?: 1}:${column ?: 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
