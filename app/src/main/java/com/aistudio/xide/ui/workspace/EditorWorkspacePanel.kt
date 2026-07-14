package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.xide.core.editor.ActiveEditorContext
import com.aistudio.xide.core.editor.CursorPosition
import com.aistudio.xide.core.vfs.VirtualFileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun EditorWorkspacePanel(
    vfs: VirtualFileSystem,
    activeFilePath: String?,
    activeEditorContextFlow: MutableStateFlow<ActiveEditorContext?>,
    onContentChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var fileContent by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }

    // Load file content when active file changes
    LaunchedEffect(activeFilePath) {
        if (activeFilePath != null) {
            isLoading = true
            try {
                val content = vfs.openFile(activeFilePath)
                fileContent = content
                textFieldValue = TextFieldValue(content)
                // Initialize context
                activeEditorContextFlow.value = ActiveEditorContext(
                    filePath = activeFilePath,
                    cursorLocation = CursorPosition(1, 1),
                    selectedText = null
                )
            } catch (e: Exception) {
                fileContent = "Error loading file: ${e.message}"
                textFieldValue = TextFieldValue(fileContent)
            } finally {
                isLoading = false
            }
        } else {
            fileContent = ""
            textFieldValue = TextFieldValue("")
            activeEditorContextFlow.value = null
        }
    }

    if (activeFilePath == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .testTag("editor_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No File Open",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a file from the Project Explorer to begin editing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Editor Header / Breadcrumbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeFilePath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("editor_active_file_header")
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != fileContent) {
                                fileContent = newValue.text
                                onContentChanged(newValue.text)
                            }

                            // Calculate cursor line & column
                            val text = newValue.text
                            val selection = newValue.selection
                            val cursorOffset = selection.start
                            
                            val textBeforeCursor = text.take(cursorOffset)
                            val line = textBeforeCursor.count { it == '\n' } + 1
                            val lastNewLineIndex = textBeforeCursor.lastIndexOf('\n')
                            val column = cursorOffset - lastNewLineIndex

                            val selectedText = if (selection.length > 0) {
                                text.substring(selection.start, selection.end)
                            } else {
                                null
                            }

                            activeEditorContextFlow.value = ActiveEditorContext(
                                filePath = activeFilePath,
                                cursorLocation = CursorPosition(line, column),
                                selectedText = selectedText
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_text_field")
                    )
                }
            }

            // Status bar at bottom of editor showing Line/Column
            val editorContext = activeEditorContextFlow.collectAsState().value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Ln ${editorContext?.cursorLocation?.line ?: 1}, Col ${editorContext?.cursorLocation?.column ?: 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("editor_status_bar")
                )
            }
        }
    }
}
