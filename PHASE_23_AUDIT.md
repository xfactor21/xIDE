# xIDE Phase 23 Pre-Implementation Audit Report

## 1. Current Architecture Map

- **Workspace Layer** (`ProjectWorkspace`, `WorkspaceContainer`): Manages project lifecycles (IDLE, LOADING, INDEXING, BUILDING, ERROR) and dynamic provider attachment.
- **VFS Boundary** (`VirtualFileSystem`, `LocalFileSystemProvider`): Provides a secured abstraction over `java.io.File`. Enforces path canonicalization and workspace root boundaries to prevent path traversal.
- **Build & Artifact Pipeline** (`BuildServiceImpl`, `GradleBuildProvider`, `APKArtifactResolver`): Restored to a non-mocked state. Executes `./gradlew` natively via `ProcessBuilder`. Securely resolves, boundaries, and validates `.apk` payloads natively from the filesystem.
- **Diagnostics Pipeline** (`DiagnosticsEngineImpl`): Intercepts `ProcessBuilder` output streams. Features regex-based credential redaction (`[REDACTED_SECRET]`), ANSI stripping, and length truncation to prevent LLM context poisoning.
- **Xero AI Orchestration** (`XeroActionEngine`, `ActionApprovalManager`): LLM prompt structuring -> JSON Validation -> Provider check -> Permission check -> ActionApprovalManager. The human-in-the-loop gate transitions intents (`PENDING` -> `APPROVED` -> `EXECUTING`).
- **Editor Analysis Pipeline** (`EditorAnalysisEngine`, `SemanticHighlightProvider`): Supplies diagnostic squiggles and semantic coloring to the IDE editor surface.

## 2. Missing Capability List

- **AST-Aware Code Intelligence**: No real compiler AST/PSI structures.
- **Language Server Protocol (LSP)**: No standardized LSP connection for robust IntelliSense.
- **Intelligent Autocomplete**: Xero can answer questions and propose patches, but lacks real-time inline type-aware autocomplete suggestions.
- **Debugger Integration**: No breakpoints, variable inspection, or execution suspension capabilities.

## 3. Technical Debt List

- **EditorAnalysisEngine Mocking**: `EditorAnalysisEngine` and `SemanticHighlightProvider` rely on elementary regex-based placeholders (e.g., matching `"TODO"` or `"class\\s+"`). They need to be refactored into a genuine syntactic tree parser.
- **Test Code Leftovers**: Certain testing structures, while ignored by production builds, retain fake protocols (`providerId: "vfs.fake"`).

## 4. Security Findings

- **VFS Path Traversal**: Secured. `VirtualFileSystem.validatePath` asserts that canonical targets always prefix-match the canonical workspace root.
- **Build Arbitrary Execution**: Secured. `GradleBuildProvider.canHandle` strictly validates `request.operation` against an explicit whitelist (`assembleDebug`, `lint`, `test`, etc.), preventing injection of arbitrary CLI flags into `./gradlew`.
- **Artifact Forgery**: Secured. `APKArtifactResolver` checks file extension, bounds, existence, and non-zero byte size before emitting an `ArtifactInfo`.
- **Context Poisoning**: Secured. Build/Compiler logs are truncated at 1000 characters and stripped of API keys.
- **Action Escalation**: Secured. Xero cannot arbitrarily mutate files or execute commands without `ActionApprovalManager` state tracking.

## 5. Recommended Phase 23 Implementation Plan

**Focus: Replacing the Editor Analysis Mocks with a Real AST Engine**

1. **Deprecate Regex Analysis**: Remove the mocked `classRegex` and `"TODO"` scanners in `EditorAnalysisEngine` and `SemanticHighlightProvider`.
2. **Implement Real Compiler Integration**: 
   - Integrate Kotlin's native `psi` (Program Structure Interface) or a lightweight subset of KSP (Kotlin Symbol Processing).
   - Alternatively, introduce a lightweight local LSP bridge capable of querying semantic tokens and symbol references.
3. **Upgrade Semantic Highlighting**: Map AST nodes (Classes, Interfaces, Functions, Annotations) directly to `SemanticHighlight` variants without string matching.
4. **Upgrade In-Editor Diagnostics**: Wire genuine `compileDebugKotlin` diagnostic line/column locations directly to the editor's live diagnostic overlay, unifying the build errors with the editor visuals.

Phase 23 is scoped specifically to resolving the structural Editor Analysis technical debt.
