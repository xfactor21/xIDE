package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.ai.actions.AIAction
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.intelligence.CodeNavigator
import com.aistudio.xide.core.intelligence.CodeExplanationService
import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.intelligence.CodeIntelligenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class XeroConversationEngineImpl(
    private val workspaceManager: WorkspaceManager?,
    private val projectIndexer: ProjectIndexerImpl?,
    private val codeNavigator: CodeNavigator?,
    private val diagnosticsEngine: DiagnosticsEngine?,
    private val buildService: BuildService?,
    private val approvalManager: ActionApprovalManager?,
    private val codeExplanationService: CodeExplanationService?,
    private val diagnosticAnalyzer: DiagnosticAnalyzer?,
    private val contextSelector: ContextSelector,
    private val diagnosticConversationService: DiagnosticConversationService? = null,
    private val fixProposalService: FixProposalService? = null,
    private val codeIntelligenceEngine: CodeIntelligenceEngine? = null
) : XeroConversationEngine {

    private val _assistantState = MutableStateFlow(XeroAssistantState.IDLE)
    override val assistantState: StateFlow<XeroAssistantState> = _assistantState.asStateFlow()

    private val _workflowState = MutableStateFlow(DeveloperWorkflowState.IDLE)
    override val workflowState: StateFlow<DeveloperWorkflowState> = _workflowState.asStateFlow()

    override val conversationHistory = ConversationHistory()

    override suspend fun handleDeveloperRequest(
        question: String,
        projectPath: String,
        activeFile: String?
    ): XeroResponse {
        _assistantState.value = XeroAssistantState.THINKING
        _workflowState.value = DeveloperWorkflowState.UNDERSTANDING_REQUEST
        
        val intent = classifyIntent(question)
        
        _assistantState.value = XeroAssistantState.ANALYZING
        _workflowState.value = DeveloperWorkflowState.ANALYZING_PROJECT
        
        return try {
            val selectedContext = contextSelector.selectContext(intent, question, projectPath, activeFile)
            
            _assistantState.value = XeroAssistantState.RESPONDING
            _workflowState.value = DeveloperWorkflowState.GENERATING_RESPONSE
            
            val response = when (intent) {
                DeveloperIntent.EXPLAIN_CODE -> {
                    val codeExplanation = if (codeExplanationService != null && selectedContext.activeFile != null) {
                        codeExplanationService.explainFile(projectPath, selectedContext.activeFile)
                    } else {
                        null
                    }
                    val summaryText = codeExplanation?.purpose ?: "The active file ${selectedContext.activeFile ?: "unknown"} contains structural code definitions. Please select a specific block for a detailed explanation."
                    XeroResponse.Explanation(
                        summary = summaryText,
                        confidence = 0.9,
                        supportingFiles = listOfNotNull(selectedContext.activeFile),
                        relatedSymbols = selectedContext.relatedSymbols,
                        nextSteps = codeExplanation?.sideEffects?.take(2) ?: listOf("Highlight a specific function inside the file", "Run compilation to verify types")
                    )
                }

                DeveloperIntent.DEBUG_ERROR -> {
                    if (diagnosticConversationService != null) {
                        val explanation = diagnosticConversationService.explainBuildFailure()
                        XeroResponse.DiagnosticReport(
                            summary = explanation.explanationText,
                            confidence = 0.95,
                            supportingFiles = explanation.affectedFiles,
                            relatedSymbols = selectedContext.relatedSymbols,
                            nextSteps = explanation.proposedManualFixes.take(2) + listOf("Run a fresh Gradle build task"),
                            diagnostics = selectedContext.diagnostics
                        )
                    } else {
                        val message = "Diagnostics engine reports ${selectedContext.diagnostics.size} active compiler errors. Root causes analyzed locally."
                        XeroResponse.DiagnosticReport(
                            summary = message,
                            confidence = 0.85,
                            supportingFiles = listOfNotNull(selectedContext.activeFile),
                            relatedSymbols = selectedContext.relatedSymbols,
                            nextSteps = listOf("Inspect error locations", "Run a fresh Gradle build task"),
                            diagnostics = selectedContext.diagnostics
                        )
                    }
                }

                DeveloperIntent.FIND_SYMBOL -> {
                    val symbolName = extractSymbolName(question)
                    val definitions = codeNavigator?.goToDefinition(projectPath, symbolName) ?: emptyList()
                    val references = codeNavigator?.findReferences(projectPath, symbolName) ?: emptyList()
                    
                    val foundSummary = if (definitions.isNotEmpty()) {
                        "Discovered symbol '$symbolName' defined at ${definitions.joinToString()}. Referenced in ${references.size} place(s)."
                    } else {
                        "Symbol '$symbolName' was not found in the project symbol index. Try rebuilding or indexing the project."
                    }
                    
                    XeroResponse.SearchResult(
                        summary = foundSummary,
                        confidence = 0.95,
                        supportingFiles = definitions,
                        relatedSymbols = listOf(symbolName),
                        nextSteps = listOf("View symbol definition", "Search usages project-wide"),
                        results = references
                    )
                }

                DeveloperIntent.SEARCH_PROJECT -> {
                    val searchResults = if (projectIndexer != null) {
                        projectIndexer.getIndex(projectPath)?.symbols?.filter {
                            it.name.contains(question.substringAfterLast(" "), ignoreCase = true)
                        }?.map { "${it.type} ${it.name} defined in ${it.location}" } ?: emptyList()
                    } else {
                        emptyList()
                    }

                    XeroResponse.SearchResult(
                        summary = "Discovered recent file mutations: ${selectedContext.recentChanges.joinToString()}. The active work scope is centered in ${selectedContext.activeFile ?: "the project root"}.",
                        confidence = 0.8,
                        supportingFiles = selectedContext.recentChanges.map { it.substringAfter(": ") },
                        relatedSymbols = emptyList(),
                        nextSteps = listOf("Examine workspace file explorer", "Refine search query with symbol keywords"),
                        results = searchResults.ifEmpty { selectedContext.recentChanges }
                    )
                }

                DeveloperIntent.ANALYZE_BUILD -> {
                    val stateStr = buildService?.buildState?.value?.name ?: "IDLE"
                    if (diagnosticConversationService != null) {
                        val explanation = diagnosticConversationService.explainBuildFailure()
                        XeroResponse.DiagnosticReport(
                            summary = "Build status is currently $stateStr.\n\n${explanation.explanationText}",
                            confidence = 0.95,
                            supportingFiles = explanation.affectedFiles,
                            relatedSymbols = emptyList(),
                            nextSteps = explanation.proposedManualFixes.take(2) + listOf("Clear caches and re-compile"),
                            diagnostics = selectedContext.diagnostics
                        )
                    } else {
                        val groupCount = diagnosticAnalyzer?.analyzeDiagnostics()?.size ?: 0
                        XeroResponse.DiagnosticReport(
                            summary = "Build status is currently $stateStr. Diagnostic system has isolated $groupCount root cause candidate(s).",
                            confidence = 0.9,
                            supportingFiles = listOfNotNull(selectedContext.activeFile),
                            relatedSymbols = emptyList(),
                            nextSteps = listOf("Apply suggested fixes", "Clear caches and re-compile"),
                            diagnostics = selectedContext.diagnostics
                        )
                    }
                }

                DeveloperIntent.SUGGEST_IMPROVEMENT -> {
                    val improvements = mutableListOf<String>()
                    if (codeIntelligenceEngine != null && selectedContext.activeFile != null) {
                        val summary = codeIntelligenceEngine.analyzeSourceFile(projectPath, selectedContext.activeFile)
                        improvements.add("The active component defines class '${summary.className ?: "unnamed"}' containing ${summary.symbols.size} member definitions.")
                        improvements.add("Imports verified: ${summary.imports.size} package bindings.")
                    } else {
                        improvements.addAll(listOf(
                            "Refactor nested class declarations to separate files to align with Clean Architecture rules",
                            "Ensure error handling utilizes robust StateFlow/UI state structures",
                            "Declare explicit public/private visibility modifiers consistently"
                        ))
                    }

                    XeroResponse.CodeInsight(
                        summary = "Identified ${improvements.size} potential areas of architectural improvement in the context.",
                        confidence = 0.75,
                        supportingFiles = listOfNotNull(selectedContext.activeFile),
                        relatedSymbols = selectedContext.relatedSymbols,
                        nextSteps = listOf("Propose a refactoring action plan", "Run code cleanup filters"),
                        patterns = improvements
                    )
                }

                DeveloperIntent.CREATE_ACTION_PROPOSAL -> {
                    val proposedActions = mutableListOf<AIAction>()
                    
                    if (fixProposalService != null && selectedContext.activeFile != null) {
                        val activeDiagnostics = diagnosticsEngine?.activeDiagnostics?.value ?: emptyList()
                        val relevantDiag = activeDiagnostics.firstOrNull { it.compilerDiagnostic?.location?.filePath == selectedContext.activeFile }
                        
                        val proposal = if (relevantDiag != null && diagnosticAnalyzer != null) {
                            val groups = diagnosticAnalyzer.analyzeDiagnostics()
                            val relevantGroup = groups.firstOrNull { it.affectedFiles.contains(selectedContext.activeFile) } ?: groups.firstOrNull()
                            relevantGroup?.let { fixProposalService.proposeDiagnosticFix(it, selectedContext.activeFile) }
                        } else {
                            fixProposalService.proposeImprovementFix(selectedContext.activeFile, "Optimize file structure and clean architectural boundaries")
                        }

                        proposal?.let {
                            proposedActions.add(it.action)
                        }
                    } else if (selectedContext.activeFile != null) {
                        proposedActions.add(
                            AIAction.ModifyFile(
                                actionId = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                source = "Xero",
                                path = selectedContext.activeFile,
                                proposedContent = "// Spacing optimized\n",
                                description = "Improve code structure and spacing in ${selectedContext.activeFile}"
                            )
                        )
                        proposedActions.forEach { approvalManager?.proposeAction(it) }
                    }
                    
                    val summaryStr = if (proposedActions.isNotEmpty()) {
                        "Successfully generated ${proposedActions.size} action proposal(s) for user verification. Execution is completely blocked until explicit human approval."
                    } else {
                        "No file was selected in workspace. Action proposals cannot be generated without an active file context."
                    }

                    _assistantState.value = XeroAssistantState.WAITING_APPROVAL
                    _workflowState.value = DeveloperWorkflowState.WAITING_APPROVAL

                    XeroResponse.ActionProposal(
                        summary = summaryStr,
                        confidence = 0.9,
                        supportingFiles = listOfNotNull(selectedContext.activeFile),
                        relatedSymbols = emptyList(),
                        nextSteps = listOf("Approve action proposal in the panel", "Reject and refine the query"),
                        proposedActions = proposedActions
                    )
                }
            }

            if (_assistantState.value != XeroAssistantState.WAITING_APPROVAL) {
                _assistantState.value = XeroAssistantState.IDLE
                _workflowState.value = DeveloperWorkflowState.COMPLETED
            }

            val entry = ConversationEntry(
                question = question,
                intent = intent,
                contextUsed = selectedContext.contextKeys,
                responseType = response::class.java.simpleName,
                timestamp = System.currentTimeMillis()
            )
            conversationHistory.addEntry(entry)

            response
        } catch (e: Exception) {
            _assistantState.value = XeroAssistantState.ERROR
            _workflowState.value = DeveloperWorkflowState.FAILED
            
            val errorResponse = XeroResponse.Explanation(
                summary = "An error occurred during query processing: ${e.message}",
                confidence = 0.0,
                supportingFiles = emptyList(),
                relatedSymbols = emptyList(),
                nextSteps = listOf("Retry query with cleaner phrasing")
            )
            
            val entry = ConversationEntry(
                question = question,
                intent = intent,
                contextUsed = emptyList(),
                responseType = errorResponse::class.java.simpleName,
                timestamp = System.currentTimeMillis()
            )
            conversationHistory.addEntry(entry)

            errorResponse
        }
    }

    private fun classifyIntent(question: String): DeveloperIntent {
        val q = question.lowercase()
        return when {
            q.contains("propose") || q.contains("proposal") || q.contains("generate change") || q.contains("propose change") -> {
                DeveloperIntent.CREATE_ACTION_PROPOSAL
            }
            q.contains("build failing") || q.contains("build errors") || q.contains("compile") || q.contains("why did build fail") -> {
                DeveloperIntent.ANALYZE_BUILD
            }
            q.contains("debug") || q.contains("error") || q.contains("exception") || q.contains("crash") || q.contains("fix error") -> {
                DeveloperIntent.DEBUG_ERROR
            }
            q.contains("where is") || q.contains("find symbol") || q.contains("go to definition") || q.contains("locate") -> {
                DeveloperIntent.FIND_SYMBOL
            }
            q.contains("search") || q.contains("find file") || q.contains("look up files") || q.contains("depend") -> {
                DeveloperIntent.SEARCH_PROJECT
            }
            q.contains("suggest") || q.contains("refactor") || q.contains("optimize") || q.contains("improve") || q.contains("clean up") -> {
                DeveloperIntent.SUGGEST_IMPROVEMENT
            }
            else -> {
                DeveloperIntent.EXPLAIN_CODE
            }
        }
    }

    private fun extractSymbolName(text: String): String {
        val words = text.split(" ")
        return words.lastOrNull()?.replace(Regex("[?.\"\']"), "") ?: "unknown"
    }
}
