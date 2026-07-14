package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.ai.actions.AIAction
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.xero.conversation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "xero"
    val text: String,
    val response: XeroResponse? = null
)

@Composable
fun XeroAssistantPanel(
    engine: XeroConversationEngine,
    approvalManager: ActionApprovalManager,
    vfs: VirtualFileSystem,
    projectRoot: String,
    activeFile: String?,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val assistantState by engine.assistantState.collectAsState()
    val workflowState by engine.workflowState.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    
    val pendingActions = remember(assistantState) {
        approvalManager.getPendingActions()
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // Status & Workflow State Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp)
                .testTag("xero_state_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Xero: $assistantState",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "State: $workflowState",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (assistantState == XeroAssistantState.WAITING_APPROVAL) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ATTENTION REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Conversation history / Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("xero_chat_history"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                val alignment = if (isUser) Alignment.End else Alignment.Start
                val bg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Render responses details
                    msg.response?.let { res ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (res.nextSteps.isNotEmpty()) {
                                    Text(
                                        text = "Next Steps:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    res.nextSteps.forEach { step ->
                                        Text(
                                            text = "• $step",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show action proposals inside the chat if waiting for approval
            if (assistantState == XeroAssistantState.WAITING_APPROVAL && pendingActions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            .padding(12.dp)
                            .testTag("xero_pending_approval_card"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pending AI Action Approval",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        pendingActions.forEach { action ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (action is AIAction.ModifyFile && action.diffModel != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Diff Preview: +${action.diffModel.addedLines} -${action.diffModel.removedLines}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            onClick = {
                                                approvalManager.approveAction(action.actionId)
                                                coroutineScope.launch {
                                                    try {
                                                        when (action) {
                                                            is AIAction.ModifyFile -> vfs.updateFile(action.path, action.proposedContent)
                                                            is AIAction.CreateFile -> vfs.createFile(action.path, action.content)
                                                            is AIAction.DeleteFile -> vfs.deleteFile(action.path)
                                                            is AIAction.RenameFile -> vfs.moveFile(action.oldPath, action.newPath)
                                                            is AIAction.MoveFile -> vfs.moveFile(action.oldPath, action.newPath)
                                                            else -> {} // No-op for read-only or unsupported actions
                                                        }
                                                        approvalManager.updateState(action.actionId, com.aistudio.xide.core.ai.actions.ActionApprovalState.COMPLETED)
                                                    } catch (e: Exception) {
                                                        approvalManager.updateState(action.actionId, com.aistudio.xide.core.ai.actions.ActionApprovalState.FAILED)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.testTag("xero_action_approve_${action.actionId}")
                                        ) {
                                            Text("Approve")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                approvalManager.rejectAction(action.actionId)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("xero_action_reject_${action.actionId}")
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input and Send row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Xero...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("xero_input_query"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        onSendMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                enabled = assistantState != XeroAssistantState.THINKING && assistantState != XeroAssistantState.ANALYZING,
                modifier = Modifier.testTag("xero_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send to Xero")
            }
        }
    }
}
