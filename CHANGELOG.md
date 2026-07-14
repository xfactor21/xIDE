# Changelog

## [Unreleased]
- **Phase 13 Completion**: Build Intelligence & Diagnostics Foundation.
  - Formulated a provider-driven `BuildProvider` and `GradleBuildProvider` to execute controlled local JVM builds and parse outputs safely.
  - Built a centralized, state-based `DiagnosticsEngine` and `DiagnosticsEngineImpl` to parse, store, and format active build warnings/errors.
  - Configured regex-based compiler parsers targeting standard Kotlin compiler diagnostics (e.g. `e: file://...: (line, col): message`) and generic warnings/errors.
  - Linked the build subsystem with the automation workflow through `BuildAutomationProvider`, mapping `AutomationAction.RunBuild` to explicit build requests.
  - Refactored `AiContextManagerImpl` to fetch parsed compiler logs and stream them to Xero's prompt snapshot context.
  - Added compatibility secondary constructor to `AiContextManagerImpl` preventing regressions in historical test suites.
  - Verified stability with JVM unit tests checking wrapper failures, automation isolation, state cleanups, and error formatting.

- **Phase 10 Completion**: Intelligence Model Integration.
  - Implemented `LanguageModelProvider` abstraction and custom `GeminiLanguageModelProvider` integrating the `gemini-3.5-flash` model.
  - Built `StructuredOutputValidator` handling JSON deserialization of LLM responses into strict domain models.
  - Introduced safety guardrails including whitelists for hallucinated capabilities and anti-injection filters for unsafe shell execution strings.
  - Integrated `XeroActionEngine` to orchestrate LLM execution -> output validation -> provider checks -> permission checks -> human-in-the-loop approvals.
  - Created custom Room tables/entities (`XeroActionEntity`, `XeroSolutionEntity`, `XeroSummaryEntity`, `XeroPreferenceEntity`) and `XeroMemoryDao` inside the `WorkspaceDatabase` schema.
  - Created `PersistentXeroMemoryContext` to hook Room storage directly into Xero memory management.
  - Wrote a full integration test suite verifying safety gates, model integration, and DB persistence.

- **Phase 9.5 Completion**: Architecture Stabilization & API Unification.
  - Replaced executable `Command` objects with immutable `ActionDescriptor`s in the planning layer.
  - Consolidated `TaskPlan` and `RecoveryPlan` into `ActionPlan`.
  - Refactored `ProjectUnderstandingEngine`, `TaskPlanningEngine`, and `RecoveryEngine` into provider-driven architectures.
  - Verified and documented `ServiceRegistry` and `PluginManager` lifecycles and boundaries.

- **Phase 9 Completion**: Intelligent Workspace Orchestration & Xero Action Engine.
  - Built `XeroActionEngine` to map Xero reasoning to `CommandSystem`.
  - Built `ProjectUnderstandingEngine` and extended `ProjectContext` for Xero.
  - Implemented `TaskPlanningEngine` for command workflows.
  - Created `ApprovalManager` for strict human-in-the-loop validation of actions.
  - Created `WorkspaceAutomationViewModel` for exposing state to UI.
  - Created `AutomationHistoryRepository` for auditing.
  - Built `RecoveryEngine` for proposing solutions to compiler errors.
  - Configured `XeroMemoryContext` boundaries.
  - Updated `ServiceRegistry` to allow `resolveByCapability` routing.

- **Phase 8 Completion**: Concrete Execution Infrastructure & Platform Integration.
  - Built `LocalTerminalProvider`, `TerminalOutputBuffer`, `LocalBuildProvider`, and `LocalPackageManagerProvider`.
  - Upgraded `TaskQueue` to `PersistentTaskQueue` using Android WorkManager.
  - Implemented `PluginRuntimeLoader` to manage real supervisor lifecycles.
  - Enhanced `ServiceRegistry` with priority and health-based resolution logic.
  - Added `ProviderHealthMonitor` for dynamic health degradation of failed plugin services.

- **Phase 7 Completion**: Plugin Engine Foundation implementation. Added PluginManifest, PluginState, PluginDescriptor, PluginManager, ServiceRegistry, PluginPermission, PluginLoader, PluginSupervisor, and Xero boundaries. Created corresponding unit tests.

- **Phase 7 Pre-Implementation**: Plugin Engine Architecture Validation complete. Documented Plugin Lifecycle, Manifest, Service Registry, Discovery, Permissions, Versioning, Failure Isolation, and Security Model.

- **Phase 6 Completion**: Automation and Build Intelligence foundation including AutomationEngine, TaskExecutor, BuildPipeline, CompilerIntegration, TerminalFoundation, PackageManagement, and XeroExecutionContext.
- **Phase 5 Completion**: Intelligence foundation including ProjectIndexer, CodeAnalysisProvider, LanguageServiceProvider, SymbolIndex, DiagnosticProvider, and XeroContextProvider.
- **Phase 4B Completion**: Editor experience layer including EditorWorkspace, EditorTabs, EditorViewport, EditorToolbar, and SyntaxHighlightProvider abstraction.
- **Phase 4A Completion**: Editor architecture foundation, text buffer abstractions, editor commands and events, document models.
- **Phase 3 Completion**: Virtual File System, Workspace Manager, Room Database for project persistence, commands, events, and hooks for Intelligence, Diagnostics, and Automation.
- **Phase 2 Completion**: App shell, design system tokens, dashboard, workspace host, responsive navigation.
- **Phase 1 Completion**: MVI UI base, provider interfaces, core command system and event bus architectures.
