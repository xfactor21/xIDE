# xIDE Architecture Document

## Overview
xIDE is an AI-powered universal mobile development operating system. It runs as a native Android application but serves as a foundational platform for various development workspaces (e.g., xForge Workspace, Android Workspace, Web Workspace, Python Workspace, AI Workspace, Game Workspace, Database Workspace). xForge is a specialized game-development workspace built on top of xIDE.

## Core Principles
1. **Platform First**: xIDE is a platform where future products run inside it. Workspaces are modular, installable, and versioned.
2. **Provider-Based Architecture**: All major subsystems communicate via abstract interfaces (Providers).
3. **Agent-Driven AI**: Xero is an autonomous engineering agent deeply integrated into the project intelligence layer.
4. **Offline First**: All core capabilities function without an internet connection.
5. **Mobile-First Engineering**: Graceful handling of limited RAM, thermal throttling, and background constraints using WorkManager and Kotlin Coroutines.
6. **Performance Observability**: Every major subsystem must expose metrics (Memory, CPU, Battery, Storage, Build duration, Indexing duration, Editor latency, Startup time, AI latency).

## Core Systems & Layers

### 1. Platform Systems
- **Core Service Registry**: Central discovery layer for providers and plugins. Responsibilities include provider registration, capability discovery, version compatibility, and plugin management.
- **Command System**: All actions must flow through a unified command architecture (e.g., Create Project, Build, Run, Undo/Redo, AI execution). Supports logging and automation.
- **Template Engine**: `TemplateProvider` system for Android, Compose, React, React Native, Flutter, Python, Node, Web, Game, Libraries, Plugins, and custom user templates.
- **Design System Architecture**: Token-based architecture (Colors, Typography, Spacing, Icons, Motion, Components, Theme states) rather than hardcoded visual styles.

### 2. Virtual File System (VFS) & Workspace Architecture
- **FileSystemProvider**: Decouples xIDE from direct storage access. Allows implementations for local files (`java.io.File`), Storage Access Framework (SAF), cloud storage, and Git-backed storage.
- **WorkspaceManager**: Manages project lifecycle (create, open, close, restore) and active session state.
- **WorkspaceRepository**: Manages Room database persistence for `ProjectEntity`, `RecentProjectEntity`, `WorkspaceSessionEntity`, and `WorkspaceMetadataEntity`.
- **Event System**: Dispatches domain events (`WorkspaceEvent`, `VfsEvent`) enabling cross-module coordination without tight coupling.

### 3. Core Operational Systems
- **`:core:projectintelligence`**: The semantic brain of xIDE. Responsibilities: Project indexing, Dependency awareness, Code relationship mapping, Architecture understanding, AI context generation, Build analysis, Error history, Project memory.
- **`:core:automation`**: Long-running operation manager. Responsibilities: Downloads, SDK installation, Builds, Updates, Repairs, Indexing, Recovery.
- **`:core:diagnostics`**: Self-monitoring and repair. Responsibilities: SDK health, Dependency conflicts, Build failures, Memory monitoring, Storage monitoring, Performance monitoring.

### 4. Provider Abstractions
Required providers include:
- `BuildProvider`
- `EditorProvider`
- `FileSystemProvider`
- `AIProvider`
- `WorkspaceProvider`
- `CompilerProvider`
- `PackageManagerProvider`
- `TerminalProvider`
- `ThemeProvider`
- `CloudProvider`
- `AuthenticationProvider`
- `PluginProvider`

### 5. Xero Architecture
Xero is an AI engineering agent with a 3-layer architecture:
- **Xero Core**: Reasoning, Memory, Context awareness, Project understanding.
- **Xero Interface**: Chat, Floating assistant, Voice, Visual states.
- **Xero Actions**: Modify files, Create projects, Execute commands, Run builds, Fix errors, Install dependencies.

### 6. UI Architecture Constraints
The UI layer strictly follows MVI/MVVM separation rules:
- **No Logic in UI**: Composable UI components must not contain business logic, build logic, file operations, AI logic, direct database access, or provider implementations.
- **Communication Flow**: UI communicates strictly through ViewModels, Use Cases, State models, and Provider interfaces.
- **State Management**: Every screen implements `UiState`, `UiEvent`, and `UiEffect` models.
- **Screen Structure**: Adheres to the pattern of Screen -> ViewModel -> State -> Actions -> Navigation Events.
- **Independence**: The design system remains independent and reusable by future workspaces and plugins.

### 7. Responsive Device Requirement
The xIDE shell is designed with adaptive Compose patterns to support a continuum of device sizes:
- Phones
- Large phones
- Foldables
- Tablets
- Desktop Android environments

## Technology Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture Pattern**: Clean Architecture & MVVM
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Async & Background**: Coroutines, Flow, WorkManager
- **Build System**: Gradle Kotlin DSL

### 8. Editor Architecture (Phase 4A)
The Editor subsystem (`core/editor`) abstracts the text editing surface from the underlying UI components. It integrates closely with the VFS and Workspace systems.
- **DocumentModel**: Represents an open file, its text buffer, language type, and save state.
- **TextBuffer**: An interface for managing scalable text content, capable of handling incremental edits and large files without full memory loading.
- **EditorCommand**: Implements the `Command` interface to track all document mutations (`InsertTextCommand`, `DeleteTextCommand`, etc.) for robust undo/redo capabilities.
- **EditorEvent**: Dispatches document lifecycle and cursor events through the EventBus.
- **EditorEngine**: Implements `EditorProvider`, serving as the central coordinator for document management.

### 9. Editor UI (Phase 4B)
The Editor UI layer (`core/editor/ui`) provides the rendering and interaction surface for the editor subsystem.
- **EditorScreen/EditorWorkspace**: Compose containers for managing layout, tabs, toolbars, and the viewport. Follows responsive layout paradigms (Compact, Medium, Expanded).
- **EditorViewport**: The rendering surface for `TextBuffer` content. Designed to be replaceable in the future (e.g. replacing `BasicTextField` with a custom canvas-based high-performance text engine).
- **EditorStateMapper**: Translates the domain `EditorState` into `EditorUiState` for compose recomposition.
- **SyntaxHighlightProvider**: An abstraction for parsing content into styled tokens, isolating language server or tokenizer implementations from the UI.

### 10. Intelligence Foundation (Phase 5)
The Intelligence Foundation provides the abstract contracts required for advanced code analysis and AI contextualization, ensuring the UI and Editor engines are entirely decoupled from language parsers or compiler APIs.
- **Project Intelligence**: `ProjectIndexer` builds `ProjectIndex` and `ProjectContext`, abstracting workspace symbol and file associations.
- **Code Analysis**: `CodeAnalysisProvider` exposes symbol discovery and metadata extraction, leveraging `LanguageDefinition`.
- **Language Services**: `LanguageServiceProvider` provides abstractions for typical LSP-like features (Syntax Analysis, Symbol Lookup, Completions).
- **Diagnostics Intelligence**: `DiagnosticProvider` standardizes error and warning reporting from compilers, static analysis, and AI suggestions.
- **Symbol Indexing**: Defines `SymbolIndex` and `SymbolRepository` contracts for abstract tracking of project-wide classes, functions, and relationships.
- **Xero Connection**: `XeroContextProvider` bridges the gap between active workspace status and the AI assistant, strictly returning structured requests and contexts.

### 11. Automation & Build Intelligence (Phase 6)
The Execution Foundation provides controlled, asynchronous mechanisms for tasks, builds, terminal execution, and dependency resolution.
- **AutomationEngine & TaskQueue**: Manages asynchronous units of work (`AutomationTask`), handling priorities and lifecycle states.
- **BuildPipeline**: Exposes `BuildProvider` and `BuildManager` for handling `BuildRequest` and returning `BuildResult`.
- **CompilerIntegration**: An abstract representation (`CompilerProvider`) for underlying language compilers.
- **TerminalExecution**: `TerminalProvider` facilitates safe session-based command runs.
- **PackageManagement**: `PackageManagerProvider` handles dependency installations modularly.
- **Diagnostics Integration**: Produces `BuildDiagnostic` items linked to `DiagnosticsEngine`.
- **Xero Integration**: `XeroExecutionContext` formulates safe environment states to propose `AutomationCommand` instances, preventing AI-driven arbitrary execution.

### 12. Service Registry (Phase 7 Design)
- **Registration**: Plugins register implementations of `XideProvider`.
- **Lookup**: Components request providers by interface class or capability string.
- **Replacement**: Priority-based. User-installed plugins can override default system providers if priorities dictate.
- **Health Tracking**: Background loops call `healthCheck()` on active providers.
- **Isolation**: Each plugin runs in a `SupervisorJob`. If a plugin crashes, the supervisor cancels its children but does not crash the `AutomationEngine` or UI shell.
- **Integration**: The `ServiceRegistry` is the sole source of truth for mapping `XideProvider` capabilities to implementations.

### 13. Failure Isolation Strategy
A broken plugin must not crash xIDE.
- **SupervisorJobs**: Plugin coroutines are scoped to isolated `SupervisorJob` contexts.
- **Health Degradation**: If a provider throws repeatedly or fails `healthCheck()`, it is automatically transitioned to `FAILED` and unregistered.
- **Graceful Degradation**: Features relying on the failed provider revert to defaults or show "Provider Unavailable" UI states.

### 14. Plugin Permissions and Sandboxing (Remaining Limitations)
Plugins do not run with implicit access.
- Every plugin operates in its own `PluginSupervisor` with an isolated coroutine scope.
- `PluginPermissionManager` enforces capability requests. Accessing core xIDE apis (VFS, Terminal) will intercept and check these permissions dynamically based on the executing plugin context.
- **In-Process Boundary**: Currently, plugins execute in-process inside the same JVM instance. The `ServiceRegistry` acts as a capability and health boundary, but does not provide complete OS-level process isolation.
- **Future OS-Level Sandboxing**: Production-ready sandboxing will require deploying plugins in `android:isolatedProcess="true"` processes, with all core-plugin communication routed through strictly validated AIDL/Binder IPC channels.

### 15. Concrete Provider Integration (Phase 8)
- **LocalTerminalProvider**: Executes raw shell commands isolated per session, feeding output to a truncated `TerminalOutputBuffer` to prevent Out-Of-Memory errors from runaway processes.
- **LocalBuildProvider**: Runs Gradle directly as a subprocess (`assembleDebug` / `assembleRelease`), intercepting streams to form structured `BuildResult` objects.
- **LocalPackageManagerProvider**: Synchronizes dependency definitions with the build system without unsafe executions.
- **PersistentTaskQueue**: Uses `androidx.work.WorkManager` to track `AutomationTask` runs reliably.
- **ProviderHealthMonitor**: Polls registered providers actively on a background coroutine, downgrading them in the `ServiceRegistry` when failures occur.

### 16. Intelligent Workspace Orchestration (Phase 9)
- **XeroActionEngine**: Maps `XeroTaskResult` into immutable `ActionDescriptor`s. Binds to `ApprovalManager` for safety.
- **ProjectUnderstandingEngine**: Generates a unified `ProjectContext` (type, languages, problems) for Xero via registered `ProjectContextProvider`s.
- **ApprovalManager**: Enforces human-in-the-loop constraints on sensitive commands.
- **RecoveryEngine**: Generates an `ActionPlan` (with commands and explanations) when an operation fails via `RecoveryStrategyProvider`s.
- **AutomationHistoryRepository**: Stores execution results (`taskId`, `userApproved`, `resultStatus`) for auditing and AI context.

### 17. Intelligence Model Integration (Phase 10)
To establish a secure, reliable, and persistent bridge between AI-driven reasoning and automation execution, xIDE integrates the following layers:
- **LanguageModelProvider**: An abstract LLM interface. Implemented by `GeminiLanguageModelProvider`, which connects to Google's `gemini-3.5-flash` model via Retrofit, configured with background thread dispatching and 60-second timeouts.
- **StructuredOutputValidator**: A strict validation boundary. Raw text responses from the model are cleaned and deserialized into concrete `ActionPlan` and `ActionDescriptor` objects. It features specialized exceptions for:
  - `MalformedOutputException`: Handles invalid or incomplete JSON parsing.
  - `HallucinatedCapabilityException`: Halts commands not registered in the static capability whitelist.
  - `UnsafeActionException`: Analyzes string inputs for bash/shell injection patterns.
- **Xero Orchestration Pipeline**: All AI proposals must sequentially pass through LLM Execution -> Structured Validation -> Provider Online Checks -> Permission Checks -> Human-in-the-Loop Approval before entering the `CommandSystem`.
- **Persistent Memory Context**: Volatile state Map references are migrated to relational SQLite Room entities (`XeroActionEntity`, `XeroSolutionEntity`, `XeroSummaryEntity`, `XeroPreferenceEntity`) queried via `XeroMemoryDao` in the unified database schema. Memory is accessed and recorded asynchronously using non-blocking suspend functions in a thread-safe `PersistentXeroMemoryContext`.


### 18. AI Intelligence & Decoupled Xero Platform Layer (Phase 11)
To ensure long-term stability, zero vendor lock-in, and extreme maintainability, xIDE implements a completely decoupled, provider-independent core AI and reasoning foundation.
- **AiProvider Abstraction**: Defines `AiProvider` extending `XideProvider`. Any backend (Gemini, OpenAI, Claude, local models) can plug in seamlessly by registering with the core.
- **Capability Routing**: `AiCapabilityRouter` dynamically maps incoming requests (such as `code_generation`, `error_analysis`, `code_explanation`) to healthy, compatible providers, raising `NoSuitableAiProviderException` if no backend satisfies the requirement.
- **Unified Context Management**: `AiContextManager` and `AiContextSnapshot` capture current files, workspace structure, diagnostics problems, and custom metadata, preventing UI components from talking directly to model providers.
- **Platform Intelligence Boundary**: `AiService` acts as the high-level functional interface (`generateCode`, `analyzeDiagnostics`, `explainCode`), isolating raw API calls and chat formatting into structured prompt-context structures.
- **Decoupled Xero Orchestration**: `XeroCore` is implemented via `XeroCoreImpl`, orchestrating the interaction between `AiService`, `AiContextManager`, workspace indexing, and `XeroMemoryContext`.


### 19. Controlled Automation Engine & Action Infrastructure (Phase 12)
xIDE establishes a strictly controlled, permission-guarded execution engine designed to perform mechanical workspace operations safely, eliminating rogue or unauthorized autonomous edits.
- **AutomationAction**: High-fidelity, immutable representations of atomic system intents (`CreateFile`, `ModifyFile`, `RunBuild`, `AnalyzeProject`, `ExecuteCommand`). Actions are purely data models describing *what* should happen, decoupled from *how* they execute.
- **AutomationResult**: Represents the unified result model containing completion state, verbose messaging, compiler/build diagnostics, and structured metadata.
- **AutomationPermissionVerifier**: A strict capability gate preventing unauthorized filesystem access or arbitrary execution outside the active workspace directory (with canonical path validation and a restriction limiting commands to Gradle-only toolchains).
- **AutomationProvider Interface**: Decoupled subsystem boundary (`FileSystemAutomationProvider`, `TerminalAutomationProvider`) allowing low-level workspace engines to implement the actual filesystem or shell interactions behind strict, mock-free platform interfaces.
- **The Xero-to-Automation Flow**: High-level `AnalysisPlan` proposals are translated via `ActionPlanner` to discrete `AutomationActions`, passed through `AutomationPermissionVerifier` verification, and executed by selected healthy providers inside `AutomationEngineImpl`.

```
XeroCoreImpl (AnalysisPlan)
        ↓
  ActionPlanner (AutomationActions)
        ↓
AutomationEngineImpl (Permission Guard)
        ↓
FileSystem / Terminal AutomationProvider
```


### 20. Build Intelligence & Diagnostics Foundation (Phase 13)
xIDE implements a robust, compiler-aware build intelligence layer designed to orchestrate Gradle execution, parse raw toolchain streams into structural diagnostics, and stream them securely to Xero's reasoning pipeline.
- **BuildRequest & BuildResult**: Strongly-typed domain intents and outcome models. Rather than relying on simple success/failure flags, `BuildResult` represents compiler status with semantic metadata, start/end timestamps, and rich compiler/build diagnostics.
- **BuildProvider & GradleBuildProvider**: Abstraction and concrete implementations of the underlying build engine. `GradleBuildProvider` implements secure wrapper discovery and local environment validation (JVM/JDK, build files), returning explicit failure diagnostics if support is incomplete instead of pretending success.
- **Diagnostics Engine**: `DiagnosticsEngine` is a centralized, stateful service tracking active compiler errors, warnings, info logs, and linter outputs. It formats these logs into descriptive context strings indicating exact file paths, lines, and columns.
- **The Decoupled Build & Diagnostics Flow**:
  1. Xero Action Planner proposes a `RunBuild` action.
  2. The `AutomationEngine` checks execution permissions and delegates to `BuildAutomationProvider`.
  3. `BuildAutomationProvider` maps the action to a structured `BuildRequest` and invokes `GradleBuildProvider`.
  4. `GradleBuildProvider` spawns local Gradle processes, streams stdout/stderr, and parses compiler logs into structured diagnostics using advanced regex pattern matchers.
  5. Parsed diagnostics are fed to the `DiagnosticsEngine`, which updates the state and streams formatted contextual summaries to `AiContextManagerImpl` for consumption by Xero's reasoning engine.

```
Xero / Action Planner (AutomationAction.RunBuild)
        ↓
   AutomationEngine (Permission Guard)
        ↓
BuildAutomationProvider (AutomationProvider)
        ↓
 GradleBuildProvider (BuildProvider & Wrapper)
        ↓
  DiagnosticsEngine (Parser) -> AiContextManager -> Xero Context
```


### 21. Build Pipeline Evidence Audit & Hardening (Phase 13.5)
To ensure xIDE's build pipeline is secure, real, and compliant, a thorough audit and hardening pass was executed:
- **Task Whitelisting & Sandboxing**: `GradleBuildProvider` has been hardened to strictly reject non-whitelisted Gradle operations inside `executeBuild`. Only the specific tasks `assembleDebug`, `compileDebugKotlin`, `test`, `lint`, and `build` are executed, entirely blocking arbitrary task or shell execution.
- **Log Sanitization & Context Security**: The `DiagnosticsEngineImpl` has been upgraded with automated log sanitization. Raw process logs are scrubbed of ANSI terminal characters, and credentials or API keys (e.g., matching typical key formats or keywords) are automatically replaced with `[REDACTED_SECRET]` tokens.
- **Buffer Limitations**: To prevent AI context bloating or denial-of-service (DoS) via huge compiler log outputs, diagnostic messages and raw outputs are strictly truncated at 1000 characters before entering the engine state.
- **Evidence-Based Artifact Discovery**: Artifact details (`ArtifactInfo`) are generated exclusively when a real compiler execution succeeds AND `apkFile.exists()` passes validation, guaranteeing no assumed or mock artifacts are reported to Xero.


