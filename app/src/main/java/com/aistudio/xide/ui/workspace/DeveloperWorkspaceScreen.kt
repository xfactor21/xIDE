package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.designsystem.layout.WindowSizeClass
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.editor.ActiveEditorContext
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.xero.conversation.XeroConversationEngine
import com.aistudio.xide.core.workspace.ui.WorkspaceNavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun DeveloperWorkspaceScreen(
    vfs: VirtualFileSystem,
    buildService: BuildService,
    diagnosticsEngine: DiagnosticsEngine,
    xeroEngine: XeroConversationEngine,
    approvalManager: ActionApprovalManager,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    projectRoot: String = "."
) {
    val coroutineScope = rememberCoroutineScope()
    val navManager = remember { WorkspaceNavigationManager() }
    val navState by navManager.navigationState.collectAsState()
    
    // Publish ActiveEditorContext
    val activeEditorContextFlow = remember { MutableStateFlow<ActiveEditorContext?>(null) }
    
    // Chat transcript for Xero Panel
    val messages = remember { mutableStateListOf<ChatMessage>() }

    // First time setup - set project details
    LaunchedEffect(Unit) {
        navManager.setCurrentProject(
            projectId = "local-project-id",
            projectName = "Local Project",
            projectRoot = projectRoot
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("developer_workspace_screen"),
        bottomBar = {
            if (windowSizeClass == WindowSizeClass.COMPACT) {
                NavigationBar {
                    NavigationBarItem(
                        selected = navState.selectedPanel == "explorer",
                        onClick = { navManager.selectPanel("explorer") },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
                        label = { Text("Explorer") },
                        modifier = Modifier.testTag("nav_item_explorer")
                    )
                    NavigationBarItem(
                        selected = navState.selectedPanel == "editor",
                        onClick = { navManager.selectPanel("editor") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Editor") },
                        label = { Text("Editor") },
                        modifier = Modifier.testTag("nav_item_editor")
                    )
                    NavigationBarItem(
                        selected = navState.selectedPanel == "xero",
                        onClick = { navManager.selectPanel("xero") },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Xero") },
                        label = { Text("Xero") },
                        modifier = Modifier.testTag("nav_item_xero")
                    )
                    NavigationBarItem(
                        selected = navState.selectedPanel == "diagnostics",
                        onClick = { navManager.selectPanel("diagnostics") },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Diagnostics") },
                        label = { Text("Problems") },
                        modifier = Modifier.testTag("nav_item_diagnostics")
                    )
                    NavigationBarItem(
                        selected = navState.selectedPanel == "build",
                        onClick = { navManager.selectPanel("build") },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Build") },
                        label = { Text("Build") },
                        modifier = Modifier.testTag("nav_item_build")
                    )
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (windowSizeClass != WindowSizeClass.COMPACT) {
                // Adaptive / Expanded layout: side rail navigation or split-pane view
                NavigationRail(
                    modifier = Modifier.width(72.dp)
                ) {
                    NavigationRailItem(
                        selected = navState.selectedPanel == "explorer",
                        onClick = { navManager.selectPanel("explorer") },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
                        label = { Text("Explorer") },
                        modifier = Modifier.testTag("nav_item_explorer")
                    )
                    NavigationRailItem(
                        selected = navState.selectedPanel == "editor",
                        onClick = { navManager.selectPanel("editor") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Editor") },
                        label = { Text("Editor") },
                        modifier = Modifier.testTag("nav_item_editor")
                    )
                    NavigationRailItem(
                        selected = navState.selectedPanel == "xero",
                        onClick = { navManager.selectPanel("xero") },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Xero") },
                        label = { Text("Xero") },
                        modifier = Modifier.testTag("nav_item_xero")
                    )
                    NavigationRailItem(
                        selected = navState.selectedPanel == "diagnostics",
                        onClick = { navManager.selectPanel("diagnostics") },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Diagnostics") },
                        label = { Text("Problems") },
                        modifier = Modifier.testTag("nav_item_diagnostics")
                    )
                    NavigationRailItem(
                        selected = navState.selectedPanel == "build",
                        onClick = { navManager.selectPanel("build") },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Build") },
                        label = { Text("Build") },
                        modifier = Modifier.testTag("nav_item_build")
                    )
                }
            }

            // Main Display Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                if (windowSizeClass != WindowSizeClass.COMPACT) {
                    // Split pane style on Expanded: Explorer + Editor always visible side-by-side or based on chosen sub-panel
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left explorer bar if selected (or always)
                        if (navState.selectedPanel == "explorer") {
                            Box(
                                modifier = Modifier
                                    .width(280.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                ProjectExplorerPanel(
                                    vfs = vfs,
                                    onFileSelected = { path ->
                                        navManager.selectFile(path)
                                        // Auto-open editor when selecting a file
                                        navManager.selectPanel("editor")
                                    }
                                )
                            }
                            VerticalDivider()
                        }

                        // Central Editor (or any fallback)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (navState.selectedPanel == "editor") {
                                EditorWorkspacePanel(
                                    vfs = vfs,
                                    activeFilePath = navState.activeFile,
                                    activeEditorContextFlow = activeEditorContextFlow,
                                    onContentChanged = { updatedContent ->
                                        coroutineScope.launch {
                                            navState.activeFile?.let { path ->
                                                vfs.updateFile(path, updatedContent)
                                            }
                                        }
                                    }
                                )
                            } else if (navState.selectedPanel == "xero") {
                                XeroAssistantPanel(
                                    engine = xeroEngine,
                                    approvalManager = approvalManager,
                                    vfs = vfs,
                                    projectRoot = projectRoot,
                                    activeFile = navState.activeFile,
                                    messages = messages,
                                    onSendMessage = { query ->
                                        messages.add(ChatMessage("user", query))
                                        coroutineScope.launch {
                                            val context = activeEditorContextFlow.value
                                            val res = xeroEngine.handleDeveloperRequest(
                                                question = query,
                                                projectPath = projectRoot,
                                                activeFile = context?.filePath
                                            )
                                            messages.add(ChatMessage("xero", res.summary, res))
                                        }
                                    }
                                )
                            } else if (navState.selectedPanel == "diagnostics") {
                                DiagnosticsPanel(
                                    diagnosticsEngine = diagnosticsEngine,
                                    onDiagnosticClick = { filePath, line ->
                                        navManager.selectFile(filePath)
                                        navManager.selectPanel("editor")
                                    }
                                )
                            } else if (navState.selectedPanel == "build") {
                                BuildPanel(
                                    buildService = buildService
                                )
                            } else {
                                // Default Explorer Panel
                                ProjectExplorerPanel(
                                    vfs = vfs,
                                    onFileSelected = { path ->
                                        navManager.selectFile(path)
                                        navManager.selectPanel("editor")
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Phone / Compact single-panel layout
                    when (navState.selectedPanel) {
                        "explorer" -> ProjectExplorerPanel(
                            vfs = vfs,
                            onFileSelected = { path ->
                                navManager.selectFile(path)
                                navManager.selectPanel("editor")
                            }
                        )
                        "editor" -> EditorWorkspacePanel(
                            vfs = vfs,
                            activeFilePath = navState.activeFile,
                            activeEditorContextFlow = activeEditorContextFlow,
                            onContentChanged = { updatedContent ->
                                coroutineScope.launch {
                                    navState.activeFile?.let { path ->
                                        vfs.updateFile(path, updatedContent)
                                    }
                                }
                            }
                        )
                        "xero" -> XeroAssistantPanel(
                            engine = xeroEngine,
                            approvalManager = approvalManager,
                            vfs = vfs,
                            projectRoot = projectRoot,
                            activeFile = navState.activeFile,
                            messages = messages,
                            onSendMessage = { query ->
                                messages.add(ChatMessage("user", query))
                                coroutineScope.launch {
                                    val context = activeEditorContextFlow.value
                                    val res = xeroEngine.handleDeveloperRequest(
                                        question = query,
                                        projectPath = projectRoot,
                                        activeFile = context?.filePath
                                    )
                                    messages.add(ChatMessage("xero", res.summary, res))
                                }
                            }
                        )
                        "diagnostics" -> DiagnosticsPanel(
                            diagnosticsEngine = diagnosticsEngine,
                            onDiagnosticClick = { filePath, line ->
                                navManager.selectFile(filePath)
                                navManager.selectPanel("editor")
                            }
                        )
                        "build" -> BuildPanel(
                            buildService = buildService
                        )
                    }
                }
            }
        }
    }
}
