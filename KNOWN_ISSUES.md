# Known Issues

This file tracks known architectural limitations, bugs, and technical debt.

## Architecture
- Currently, all core interfaces are stubbed inside the `app` module for Phase 1 compilation. True multi-module segregation is planned but deferred to avoid immediate build complexity until interfaces stabilize.

## Build System

## Dependencies
- Navigation Compose and Hilt dependencies have been added. 
- **CRITICAL**: The `com.google.dagger.hilt.android` Gradle plugin is temporarily disabled. It relies on `BaseExtension` which was removed in AGP 9.0. Once Hilt releases a compatible update for AGP 9.1.1, the plugin should be re-enabled and `@HiltAndroidApp` / `@AndroidEntryPoint` uncommented. For Phase 1, structural interfaces are intact.

## Virtual File System (VFS)
- **SAF Integration**: `LocalFileSystemProvider` currently relies on standard `java.io.File` APIs. This will violate Scoped Storage on modern Android devices when accessing external directories. Future phases must migrate this to Storage Access Framework (SAF) which introduces asynchronous document trees and permission intents.
- **VFS Transaction Locking**: Concurrent quick-succession writes could benefit from transactional read/write locking channels to prevent race conditions during heavy parallel file access.
- **Rollback Limitations**: `ChangeHistory` tracks file change rollbacks linearly via an in-memory stack. If a user manually edits a file outside the AI workflow, or if files are modified out of order, reverting changes can cause merge conflicts or overwrite subsequent modifications. Future updates should introduce semantic visual diff resolution or Git-based branch checkpoints.

## Project Indexing
- **Regex-Based AST Approximation**: The lightweight indexing engine parses symbols via optimized regex. While extremely performant, lightweight, and offline-friendly, it does not build a full AST semantic graph, meaning complex inline nested classes, multiline function parameters, or multi-file alias imports might be unresolved.
- **Line-by-Line Parsing Limitations**: Multi-line structures (e.g. annotations on previous lines) are handled via stateful pending buffers, but complex multi-statement single-line definitions or heavily nested blocks can still cause minor matching drift compared to a compiler-grade AST parser.

## Room Database
- **Migrations**: WorkspaceDatabase is currently on version 1. Future iterations will require explicit Room migrations when schemas change.

## Editor Architecture
- **Text Rendering**: `EditorViewport` currently uses `BasicTextField`. This will struggle with very large text files (e.g., >10MB) or complex syntax span applications. Future iterations will require a custom canvas-based high-performance text engine.
- **Syntax Highlighting**: `SyntaxHighlightProvider` currently falls back to a plain text implementation.

## Xero Assistant Interface
- **Rule-Based Intent Classification**: The current local, offline-first intent classifier uses highly optimized regex and keyword matching. Highly ambiguous, multi-intent, or non-English developer phrasing might default to the `EXPLAIN_CODE` fallback intent. Future phases can integrate a semantic vector matching or a small local transformer model to handle highly complex natural language variations.
- **Active Context Truncation**: To prevent out-of-memory states, excessive token consumption, or AI context bloat, the `ContextSelector` truncates file snippets at 30 lines. Complex multi-file refactoring queries requiring large context windows may need more targeted sub-queries or a sliding window context selector in later phases.

## Workspace UI
- **Large Action Diffs**: When Xero proposes extremely large file modifications, the `ActionProposalCard` simply prints a textual description without presenting a visual side-by-side split diff. Advanced visual diffing should be introduced before launching complex autonomous coding agents.

## Code Intelligence
- **Remaining Parser Limitations**: The lightweight semantic symbol extraction (regex-based) does not build a full AST. It cannot accurately resolve fully qualified paths for overloaded extension functions or correctly infer return types without explicit annotations. Future phases may require KSP or Kotlin Compiler API integration for perfect type resolution.

## Testing
- **KSP NullPointerException**: A background unit test runner occasionally produces a `NullPointerException` due to `ksp.com.intellij.openapi.application.Application.getService` during compilation cache builds. This does not affect successful test executions.

### Phase 22 Semantic Intelligence Limitations
*   **Parser limitations**: `SemanticHighlightProvider` and `EditorAnalysisEngine` rely on lightweight regex matching rather than a full AST or Kotlin Compiler PSI, meaning complex nested structures or multiline declarations might be misclassified.
*   **Performance constraints**: While analysis runs off the main thread, extremely large files (e.g. 5,000+ lines) might experience delayed context extraction or highlight updates. Full AST debouncing will be needed in future phases.
*   **Semantic analysis boundaries**: `CodeImprovementAnalyzer` does not currently hook into a language server (LSP); it relies on hardcoded pattern matching (e.g., detecting empty catch blocks).
