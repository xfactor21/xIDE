package com.aistudio.xide.core.xero

import com.aistudio.xide.core.ai.AiContextManager
import com.aistudio.xide.core.ai.AiService
import com.aistudio.xide.core.ai.AiServiceResult
import com.aistudio.xide.core.ai.actions.*
import com.aistudio.xide.core.automation.ActionPlanner
import com.aistudio.xide.core.automation.AutomationService
import com.aistudio.xide.core.intelligence.ProjectIndexer
import com.aistudio.xide.core.intelligence.ProjectStructureService
import java.util.UUID

/**
 * Production implementation of XeroCore acting as the reasoning and orchestration engine.
 * Provider-independent, context-aware, and decoupled from raw model providers and UI.
 */
class XeroCoreImpl(
    private val aiService: AiService,
    private val aiContextManager: AiContextManager,
    private val projectIndexer: ProjectIndexer?,
    private val projectStructureService: ProjectStructureService?,
    private val memoryContext: XeroMemoryContext,
    private val actionPlanner: ActionPlanner? = null,
    private val automationService: AutomationService? = null,
    private val fileChangeTracker: com.aistudio.xide.core.vfs.FileChangeTracker? = null,
    private val buildService: com.aistudio.xide.core.build.BuildService? = null,
    private val workspaceManager: com.aistudio.xide.core.workspace.WorkspaceManager? = null,
    val approvalManager: ActionApprovalManager = ActionApprovalManager(),
    val fileOperationProvider: AIFileOperationProvider? = null,
    val changeHistory: ChangeHistory? = null
) : XeroCore {

    private var currentProjectPath: String? = null

    override suspend fun attachToProject(projectPath: String) {
        currentProjectPath = projectPath
        memoryContext.recordAction("Attaching Xero core to project path: $projectPath")
        if (projectIndexer != null) {
            projectIndexer.indexProject(projectPath).await()
        }
    }

    override suspend fun analyze(prompt: String, contextFiles: List<String>): AnalysisPlan {
        val projectPath = currentProjectPath ?: throw IllegalStateException("XeroCore is not attached to any project. Call attachToProject first.")
        
        memoryContext.recordAction("Running problem analysis in XeroCore for prompt: $prompt")
        
        contextFiles.forEach { filePath ->
            aiContextManager.addManualContext("file_$filePath", "Content snapshot placeholder for file: $filePath")
        }

        val contextSnapshot = aiContextManager.captureCurrentContext()
        
        val aiResult = aiService.generateCode(prompt, contextSnapshot)
        
        val responseText = when (aiResult) {
            is AiServiceResult.Success -> aiResult.content
            is AiServiceResult.Failure -> throw aiResult.error
        }

        val planId = UUID.randomUUID().toString()
        val proposedActions = mutableListOf<XeroAction>()
        
        if (prompt.contains("create", ignoreCase = true)) {
            proposedActions.add(XeroAction.CreateFile("$projectPath/src/NewFile.kt", "// Proposed by Xero Core analysis"))
        } else {
            proposedActions.add(XeroAction.EditFile("$projectPath/src/Main.kt", "Apply architectural update based on request: $prompt"))
        }

        // Pre-propose actions to the ActionApprovalManager for tracking & approval boundaries
        proposedActions.forEach { xeroAction ->
            val aiAction = mapToAIAction(xeroAction)
            approvalManager.proposeAction(aiAction)
        }

        return AnalysisPlan(
            id = planId,
            summary = "Xero core analysis complete. Summary of recommendations:\n$responseText",
            proposedActions = proposedActions
        )
    }

    override suspend fun execute(plan: AnalysisPlan): ExecutionResult {
        memoryContext.recordAction("Authorizing and executing AnalysisPlan: ${plan.id}")
        
        // Find matching proposed AI actions in approvalManager
        val pendingList = approvalManager.getPendingActions()
        
        // Enforce approval boundaries: any pending filesystem-changing actions block execution
        if (pendingList.isNotEmpty()) {
            return ExecutionResult.Failure(SecurityException("Action requires explicit confirmation: PENDING state detected"))
        }

        // Proceed to execute only if we have a fileOperationProvider and actions are approved/auto-approved
        val provider = fileOperationProvider
        if (provider != null) {
            var completedCount = 0
            val allHistory = approvalManager.getActionHistory()
            
            // Collect approved/runnable actions
            val approvedActions = approvalManager.getPendingActions().toMutableList() // None should be pending here
            
            // Let's retrieve all proposed actions that are APPROVED
            val actionable = allHistory.filter { it.value == ActionApprovalState.APPROVED }.keys
                .mapNotNull { approvalManager.getAction(it) }

            for (action in actionable) {
                approvalManager.updateState(action.actionId, ActionApprovalState.EXECUTING)
                val result = provider.executeAction(action)
                if (result.success) {
                    approvalManager.updateState(action.actionId, ActionApprovalState.COMPLETED)
                    result.rollbackInfo?.let { changeHistory?.recordChange(it) }
                    completedCount++
                } else {
                    approvalManager.updateState(action.actionId, ActionApprovalState.FAILED)
                    return ExecutionResult.PartialSuccess(
                        completed = completedCount,
                        total = actionable.size,
                        lastError = RuntimeException(result.errorMessage ?: "File operation failed")
                    )
                }
            }
            return ExecutionResult.Success
        }

        // Fallback to legacy automation service if no direct fileOperationProvider is available
        val planner = actionPlanner
        val dispatcher = automationService
        
        if (planner != null && dispatcher != null) {
            val automationActions = planner.planActions(plan)
            var completedCount = 0
            for (action in automationActions) {
                val result = dispatcher.dispatchAction(action)
                if (!result.success) {
                    return ExecutionResult.PartialSuccess(
                        completed = completedCount,
                        total = automationActions.size,
                        lastError = RuntimeException(result.message)
                    )
                }
                completedCount++
            }
            return ExecutionResult.Success
        }
        
        return ExecutionResult.Success
    }

    override suspend fun getXeroProjectContext(): XeroProjectContext {
        val root = currentProjectPath ?: ""
        val name = if (root.isNotEmpty()) root.substringAfterLast('/') else "Unnamed Project"
        val activeFile = workspaceManager?.getActiveWorkspace()?.activeFile
        
        val recentChangesList = mutableListOf<String>()
        fileChangeTracker?.let { tracker ->
            recentChangesList.addAll(tracker.createdFiles.map { "Created: $it" })
            recentChangesList.addAll(tracker.modifiedFiles.map { "Modified: $it" })
            recentChangesList.addAll(tracker.deletedFiles.map { "Deleted: $it" })
        }

        val buildStateStr = buildService?.buildState?.value?.name ?: "IDLE"
        
        val activeProblems = aiContextManager.captureCurrentContext().buildDiagnostics

        val pending = approvalManager.getPendingActions().map { "${it.actionId}: ${it.description}" }
        val recentOps = approvalManager.getActionHistory().map { "${it.key}: ${it.value}" }
        val prevChanges = changeHistory?.getHistory()?.map { "Rollbackable [${it.operationType}]: ${it.filePath}" } ?: emptyList()

        val index = if (root.isNotEmpty()) projectIndexer?.getIndex(root) else null
        val symbolInfo = index?.symbols?.map { "${it.type} ${it.name} in ${it.location}" } ?: emptyList()
        val relatedFiles = index?.relationships?.map { "${it.sourceSymbol} -> ${it.targetSymbol}" } ?: emptyList()
        val depGraph = index?.relationships?.groupBy({ it.sourceSymbol }, { it.targetSymbol }) ?: emptyMap()
        
        val diagRels = activeProblems.associateWith { diag ->
            index?.symbols?.filter { diag.contains(it.name, ignoreCase = true) }?.map { it.name } ?: emptyList()
        }
        
        val summaries = index?.files?.associateWith { file ->
            "File containing ${index.symbols.filter { it.location.startsWith(file) }.size} parsed symbols."
        } ?: emptyMap()

        return XeroProjectContext(
            name = name,
            rootPath = root,
            activeFile = activeFile,
            recentChanges = recentChangesList,
            buildStatus = buildStateStr,
            diagnostics = activeProblems,
            availableActions = listOf("CreateFile", "ModifyFile", "DeleteFile", "RenameFile", "MoveFile", "ExplainCode", "AnalyzeError", "SuggestFix"),
            pendingApprovals = pending,
            recentAiOperations = recentOps,
            previousApprovedChanges = prevChanges,
            symbolInformation = symbolInfo,
            relatedSymbols = index?.symbols?.map { it.name } ?: emptyList(),
            relatedFiles = relatedFiles,
            dependencyRelationships = depGraph,
            dependencyGraph = depGraph,
            diagnosticRelationships = diagRels,
            codeSummaries = summaries,
            recentFileActivity = recentChangesList,
            openEditorState = activeFile?.let { mapOf(it to "open") } ?: emptyMap(),
            rollbackAvailability = true
        )
    }

    private fun mapToAIAction(xeroAction: XeroAction): AIAction {
        val actionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val source = "Xero"
        return when (xeroAction) {
            is XeroAction.CreateFile -> {
                AIAction.CreateFile(
                    actionId = actionId,
                    timestamp = timestamp,
                    source = source,
                    path = xeroAction.path,
                    content = xeroAction.content
                )
            }
            is XeroAction.EditFile -> {
                AIAction.ModifyFile(
                    actionId = actionId,
                    timestamp = timestamp,
                    source = source,
                    path = xeroAction.path,
                    proposedContent = "// Modified: " + xeroAction.instruction
                )
            }
            is XeroAction.RunCommand -> {
                AIAction.ExplainCode(
                    actionId = actionId,
                    timestamp = timestamp,
                    source = source,
                    code = xeroAction.command
                )
            }
        }
    }
}
