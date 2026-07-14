# xIDE API Contracts

This document outlines the core public interfaces that form the foundation of the xIDE platform. These contracts guarantee stability for future workspaces and plugins.

## Core Provider Interface
Every provider in the system must implement the base `XideProvider` interface, which defines its identity, capabilities, lifecycle, and metadata.

## Service Registry
The `ServiceRegistry` is the central discovery layer. It allows registering, querying, and managing providers and plugins.

## Event System
The `EventBus` provides a decoupled communication mechanism using `PlatformEvent` and `EventListener`.

## Command System
The `CommandSystem` handles all user and AI actions through a unified execution architecture supporting undo, redo, and logging.

## Virtual File System (VFS)
The `FileSystemProvider` extends `XideProvider` and abstracts direct storage access.
- `list(path)`
- `readFile(path)`
- `writeFile(path)`
- `delete(path)`
- `mkdir(path)`
- `exists(path)`

## Workspace Management
The `WorkspaceManager` extends `XideProvider` and handles project lifecycle.
- `createProject(name, path)`
- `openProject(projectId)`
- `closeCurrentProject()`
- `getActiveWorkspace()`

## Workspace Persistence
The `WorkspaceRepository` abstracts database access for workspace data.
- Manage `ProjectEntity`, `RecentProjectEntity`
- Manage `WorkspaceSessionEntity` (open files, active file)
- Manage `WorkspaceMetadataEntity`

## Editor Architecture
The `EditorProvider` (implemented by `EditorEngine`) abstracts text editing operations.
- `contentFlow`: Exposes the document content as a StateFlow.
- `openFile(filePath)`
- `saveFile()`
- `insertText(text, position)`
- `replaceText(start, end, text)`

## Editor UI Integration
The Editor UI interacts with the domain via specific event callbacks:
- `onContentChanged(String)`
- `onTabSelected(String)`
- `onTabClosed(String)`
- `onSave()`, `onUndo()`, `onRedo()`
The UI layer is strictly separated from `EditorProvider` implementation.

## Intelligence Providers
- `ProjectIndexer`: Handles project metadata extraction and relationship tracking. Outputs `ProjectContext`.
- `CodeAnalysisProvider`: Performs static analysis. Discovering `ProjectSymbol`s.
- `LanguageServiceProvider`: Interface analogous to LSP handling completion, navigation, and syntax semantics.
- `DiagnosticProvider`: Unifies `DiagnosticItem` tracking, publishing a Flow of diagnostics for specific files.
- `XeroContextProvider`: Extracts necessary `ProjectContext` for Xero analysis requests.

## Automation and Build Providers
- `AutomationEngine`: Coordinates `TaskQueue` and `AutomationTask` lifecycles.
- `TaskExecutor`: Defines capabilities and bounds for specific background task handlers.
- `BuildProvider`: Pluggable compiler/build tool integration.
- `CompilerProvider`: Language-specific compiler integration capabilities.
- `TerminalProvider`: Session-based execution for abstracted shell commands.
- `PackageManagerProvider`: Facilitates resolution and installation of external dependencies.

## Plugin Architecture Contracts
- `xide-plugin.json`: The standard manifest schema required for all plugins, defining identity, compatibility, exported providers, and permissions.
- `ServiceRegistry`: The central hub where `XideProvider` instances from plugins are registered and resolved by the rest of the application.

## Phase 7 Plugin Contracts
- `PluginManifest`: Formal contract representing plugin metadata, exported providers, and requested capabilities.
- `PluginManager`: Lifecycle governor orchestrating validation, enablement, and disabling of plugins.
- `PluginPermissionManager`: Enforces explicitly requested bounds for any dynamic operations.
- `ServiceRegistry`: Central locator for mapping plugin implementations to core framework capabilities.
- `XeroPluginContext`: The exact boundary object provided to Xero to observe the current plugin state securely.

## Phase 8 Provider Contracts
- `LocalTerminalProvider`, `LocalBuildProvider`, `LocalPackageManagerProvider`: Specific default implementations of Phase 6 foundational API contracts.
- `PersistentTaskQueue`: Standardizes how work enters Android's WorkManager under our Custom `AutomationTask` definition.

## Phase 10 Intelligence Contracts (New)
- `LanguageModelProvider`: Abstraction for calling Large Language Models.
  - `generate(ModelRequest): ModelResponse`
- `StructuredOutputValidator`: Converts LLM responses into structured, safe domain models.
  - `validateAndParse(String): ActionPlan`
- `XeroMemoryDao`: Room DAO managing persistent entities for actions, solutions, summaries, and preferences.
- `XeroMemoryContext` / `PersistentXeroMemoryContext`: Concrete repository bridge integrating persistent memory into Xero.
- `XeroActionEngine`: Pipeline orchestrating LLM request -> validation -> permission interception -> human-in-the-loop approval gates.

