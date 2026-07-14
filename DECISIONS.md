# Architectural Decisions Record

## 1. Naming & Identity
**Decision**: The universal development environment is named `xIDE`. `xForge` is designated as a specialized workspace/plugin built on top of `xIDE`.
**Reason**: Clarifies the platform vs product distinction, allowing xIDE to host various development environments beyond game creation.

## 2. Hybrid Build Strategy
**Decision**: Architect for both Local and Cloud builds via a `BuildProvider` interface.
**Reason**: Avoids vendor lock-in and provides fallback mechanisms for low-end mobile devices while utilizing cloud compute for heavy builds.

## 3. Abstract Editor & File System
**Decision**: Decouple the editor and file system using `EditorProvider` and `FileSystemProvider`.
**Reason**: Ensures the UI component or underlying storage mechanism can be swapped out (e.g., native compose vs Monaco, local vs Git storage) without breaking the application logic.

## 4. Xero as an Autonomous Agent
**Decision**: Xero will have a 3-layer architecture (Core, Interface, Actions) and will function as an autonomous engineering agent capable of executing commands.
**Reason**: Moves Xero from a conversational chat UI to an embedded developer that can actively diagnose and resolve project issues.

## 5. Automation & Diagnostics
**Decision**: Background tasks are handled by an `AutomationEngine`, and system health is monitored by a `DiagnosticsEngine`.
**Reason**: Mobile constraints demand robust, resumable operations for downloads and builds, and self-healing systems prevent corruption.

## 6. Room over SharedPreferences for Workspace Data
**Decision**: Use Room Database with relational entities (`ProjectEntity`, `RecentProjectEntity`, `WorkspaceSessionEntity`, `WorkspaceMetadataEntity`) for managing project state instead of SharedPreferences or plain JSON files.
**Reason**: Provides transaction safety, complex querying, relational integrity (cascading deletes), and structured schema migrations for long-term platform evolution.

## 7. Event-Driven Workspace State
**Decision**: Use an event bus mechanism (`WorkspaceEvent`, `VfsEvent`) instead of tight callbacks for the VFS and Workspace lifecycle.
**Reason**: Allows decoupled systems (Intelligence, Diagnostics, Automation, Xero) to react to file and project changes without creating massive dependency cycles.

## 8. Explicit Command Pattern for VFS Mutations
**Decision**: All mutations to the file system or workspace (`CreateProjectCommand`, `MoveFileCommand`, etc.) implement a strict `Command` interface.
**Reason**: Lays the foundation for multi-level Undo/Redo, operation batching, logging, and allows Xero (the AI) to securely execute mutations using standard channels.

## 9. Decoupled TextBuffer Abstraction
**Decision**: The text editing core relies on a `TextBuffer` interface instead of primitive Strings or Android's `Editable`.
**Reason**: Allows for future implementation of advanced data structures (like Piece Tables or Ropes) to efficiently handle very large files and complex undo trees without blocking the UI thread.

## 10. Command-Driven Editor Mutations
**Decision**: Every text change (Insert, Delete, Replace) is modeled as an `EditorCommand`.
**Reason**: Ensures uniformity with the global Command System, enabling a universal Undo/Redo stack and allowing Xero (AI Agent) to propose granular, revertible text edits.

## 11. Abstract Intelligence Foundation
**Decision**: Implementing `ProjectIndexer`, `LanguageServiceProvider`, and `CodeAnalysisProvider` as abstract layers prior to integrating a real parsing engine (e.g. Tree-sitter or compiler API).
**Reason**: Strict decoupling. Ensures the IDE Application Shell, VFS, and Editor do not bind themselves to any specific language logic, keeping xIDE natively language-agnostic and plugin-ready.

## 12. Abstracting the Xero Context Layer
**Decision**: Xero's AI interactions are bottlenecked through `XeroContextProvider` which produces a declarative `ProjectContext` for analysis requests.
**Reason**: Prevents the AI engine from executing uncontrolled deep reads against the file system.

## 13. Async Automation Engine
**Decision**: Enforcing `TaskQueue` and `AutomationEngine` for all executions instead of ad-hoc coroutines.
**Reason**: Traceability and lifecycle management. It allows the IDE to persist background progress, handle graceful shutdowns, and eventually map tasks to Android's `WorkManager` for resilience.

## 14. Decoupled Terminal Sessions
**Decision**: `TerminalSession` acts as a stateful abstraction for shell interaction.
**Reason**: Prevents direct UI threading blocks and prepares the architecture for cloud-hosted container terminals in future phases.

## 15. Plugin Discovery & Lifecycle
**Decision**: Plugins will be discovered via VFS scanning and managed through a strict 9-state lifecycle (`DISCOVERED` to `REMOVED`).
**Reason**: Prevents uncontrolled execution and ensures all plugins pass a validation gate before their classloader is initialized.

## 16. Plugin Security Boundary
**Decision**: Plugins must request explicit capability strings (`vfs_write`, `terminal_execute`) in their manifest, and Xero can only interact with plugins by proposing commands.
**Reason**: Enforces the principle of least privilege and prevents Xero from bypassing the CommandSystem's permission interceptors.

## 17. Plugin Supervisor Boundary
**Decision**: Plugins execute their background providers via `PluginSupervisor` scoping.
**Reason**: To prevent a single poorly written community plugin from tearing down the application `CoroutineScope` through unhandled exceptions.

## 18. WorkManager for AutomationEngine
**Decision**: Adopted `androidx.work.WorkManager` within the `PersistentTaskQueue`.
**Reason**: To ensure guaranteed task delivery and resume capability for compilation and dependency operations if the application shell is background-killed by the OS.

## 19. Terminal Output Buffering
**Decision**: Cap terminal output at 1000 lines strictly using `ConcurrentLinkedDeque` (`TerminalOutputBuffer`).
**Reason**: Uncapped compiler/shell logs easily crash Android applications due to GC pressure.

## 20. Xero Action Boundary
**Decision**: Xero produces immutable `ActionDescriptor` models representing intended operations, rather than directly invoking concrete executable `Command`s.
**Reason**: Strict decoupling. Planning layers should not hold execution references. Actual resolution of an ActionDescriptor to a Command occurs immediately prior to execution, maintaining safety and predictable state.

## 21. ActionPlan Consolidation
**Decision**: Merged `TaskPlan` and `RecoveryPlan` into a unified `ActionPlan` model.
**Reason**: Deduplicates data models in the intelligence layer. Both engines fundamentally describe a goal and a proposed list of steps requiring user approval.

## 22. Provider-Driven Intelligence
**Decision**: `TaskPlanningEngine`, `RecoveryEngine`, and `ProjectUnderstandingEngine` aggregate domain logic through registered provider extensions instead of hardcoding rules.
**Reason**: Stabilizes the core APIs and pushes domain-specific implementations (e.g. Kotlin compiler recovery, Node task planning) out to plugins, adhering to the platform-first directive.

## 23. Structured Output Deserialization for AI Actions
**Decision**: Enforced that LLM integration output can only be deserialized into structured `ActionDescriptor` and `ActionPlan` objects via `StructuredOutputValidator`.
**Reason**: To maintain strict boundaries between AI-assisted reasoning and automation execution. The LLM is completely isolated from command executing contexts, and raw text is never parsed ad-hoc.

## 24. Defensive Execution Guardrails & Exception Gates
**Decision**: Structured AI outputs are processed through multiple sequential validation gates (Malformed Output, Hallucinated Capabilities, Unavailable Providers, Unsafe Command Injection, Invalid Permissions) that throw specialized, interceptable exceptions.
**Reason**: Guarantees system integrity, protects the terminal from shell injection attacks, halts illegal action proposals before they reach the user interface, and enforces the principle of least privilege.

## 25. Persistent Room-backed Xero Memory Storage
**Decision**: Migrated the volatile, in-memory Map of Xero context state into database-backed persistent entities (`XeroActionEntity`, `XeroSolutionEntity`, `XeroSummaryEntity`, `XeroPreferenceEntity`) handled by a dedicated `XeroMemoryDao` inside the unified `WorkspaceDatabase` schema.
**Reason**: Ensures the AI agent's learnings, problem solutions, project understandings, and user preferences are fully preserved across app restarts and workspace switches.

## 26. In-Process Plugin Boundary with Deferred OS-level Sandbox
**Decision**: Standardized on in-process plugin management via the central `ServiceRegistry` capability & permission verification gates, with full OS-level isolated process sandboxing deferred to a subsequent phase.
**Reason**: High development speed for Phase 10 validation. Moving to an isolated process architecture immediately introduces heavy AIDL/IPC serialization overhead and binder-to-binder context-switching latency. The in-process verification provides robust logical safety checks for all command resolutions.


## 27. Provider-Independent AI and Decoupled Orchestration Layer
**Decision**: Establish a provider-independent core AI layer (`core/ai/`) utilizing dynamic `AiCapabilityRouter` capability routing, unified `AiContextManager` snapshots, and high-level `AiService` boundaries. 
**Reason**: Strict decoupling and platform-first scalability. By abstracting raw model providers from the core, xIDE has zero vendor lock-in, enabling seamless registration of different LLM backends (Gemini, OpenAI, Claude, local models) without modifying core business logic.


## 28. Controlled Automation Engine & Action Infrastructure
**Decision**: Introduce a highly-guarded, permission-gated automation infrastructure (`core/automation/`) centered around immutable, non-executing `AutomationAction` models, separate high-level planning boundaries, and decoupled low-level subsystem providers.
**Reason**: To establish hands for xIDE that can perform files and compiler tasks under complete human control without any risk of rogue, unsafe, or unauthorized autonomous modifications. Canonical path verification and command restriction limits the scope strictly to Gradle commands in a safe, mock-free environment.


## 29. Compiler-Aware Build Intelligence and Central Diagnostics Engine
**Decision**: Establish a provider-based Build System (`core/build/`) and a centralized stateful `DiagnosticsEngine` (`core/diagnostics/`) that parse compiler logs in real-time, completely decoupling core logic from Gradle CLI details.
**Reason**: Strict modularity and enhanced reasoning capabilities. Having a dedicated build abstraction protects xIDE from tying itself to operating system process parameters, and the centralized diagnostics engine establishes a structural context pathway (Compiler -> Parser -> Context Manager -> Xero) allowing Xero to locate and comprehend compile errors down to specific files, lines, and columns.


## 30. Build Pipeline Whitelisting and Real-Time Log Sanitization
**Decision**: Enforce a strict task whitelist inside `GradleBuildProvider.executeBuild` and execute automated cleaning, redacting, and size-limiting procedures within the central `DiagnosticsEngineImpl`.
**Reason**: To guarantee the execution of Gradle remains completely secure against task-injection attacks or process manipulation, and ensure that ANSI colors, sensitive tokens (API keys/secrets), or massive trace files never pollute or overflow the AI reasoning context. This builds absolute trust in the AI-assisted developer loop.


## 31. Evidence-Based APK Artifact Resolver & Build State Tracking
**Decision**: APK builds are evidence-based and require verified Gradle output, introducing a dedicated `ArtifactResolver` and tracking state lifecycle with `BuildService`.
**Reason**: To avoid "fake success" claims or simulated build results. The system demands that a real Gradle process must run to completion with an exit code of `0`, and a real `.apk` binary file with non-zero size must physically exist in the project's canonical output directory before any artifact is reported to the user or Xero.





