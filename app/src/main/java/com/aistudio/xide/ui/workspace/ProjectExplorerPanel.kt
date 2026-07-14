package com.aistudio.xide.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.vfs.VirtualFileSystem
import kotlinx.coroutines.launch

@Composable
fun ProjectExplorerPanel(
    vfs: VirtualFileSystem,
    onFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf(vfs.getActiveWorkspaceRoot() ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var filesList by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    var searchedFiles by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load active path files
    LaunchedEffect(currentPath) {
        if (currentPath.isNotEmpty()) {
            isLoading = true
            try {
                filesList = vfs.listFiles(currentPath)
            } catch (e: Exception) {
                filesList = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // Handle search query updates
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isLoading = true
            try {
                searchedFiles = vfs.searchFiles(searchQuery)
            } catch (e: Exception) {
                searchedFiles = emptyList()
            } finally {
                isLoading = false
            }
        } else {
            searchedFiles = emptyList()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Files") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("project_explorer_search_input"),
            singleLine = true
        )

        if (searchQuery.isEmpty()) {
            // Show directory path & back navigation
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val workspaceRoot = vfs.getActiveWorkspaceRoot() ?: ""
                if (currentPath != workspaceRoot && currentPath.length > workspaceRoot.length) {
                    IconButton(
                        onClick = {
                            val parent = currentPath.substringBeforeLast("/")
                            if (parent.length >= workspaceRoot.length) {
                                currentPath = parent
                            }
                        },
                        modifier = Modifier.testTag("explorer_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
                Text(
                    text = currentPath.substringAfterLast("/", currentPath),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp).testTag("explorer_current_dir")
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val displayList = if (searchQuery.isNotEmpty()) searchedFiles else filesList
            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).testTag("explorer_file_list")) {
                    items(displayList) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (node.isDirectory) {
                                        currentPath = node.path
                                    } else {
                                        onFileSelected(node.path)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("explorer_item_${node.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = if (node.isDirectory) "Folder" else "File",
                                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!node.isDirectory) {
                                    Text(
                                        text = "${node.size} bytes",
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
    }
}
