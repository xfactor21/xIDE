# xIDE Security Architecture
This document defines the security principles and foundation for the xIDE platform.

## Principles
1. **Credential Storage**: Secure storage strategy utilizing encrypted DataStore and Android Keystore for sensitive data.
2. **API Key Protection**: Strict isolation of platform and user API keys. Keys are not hardcoded or exposed in plaintext logs.
3. **Plugin Permissions**: Future plugins will operate under a strict permission model. Plugins must declare required capabilities (e.g., file access, network) before execution.
4. **Workspace Isolation**: Workspaces execute within their own sandbox context to prevent cross-workspace contamination or unauthorized access.
5. **File Access Permissions**: The FileSystemProvider enforces scope-based access to the project directory and prevents traversal outside the workspace boundary.
6. **Future Sandboxing Model**: AI agents and third-party plugins will run in isolated environments to ensure platform stability and security.

## Phase 3 Updates
7. **Local Storage Integrity**: `WorkspaceDatabase` currently exports schema in plaintext, but sensitive project configurations should be encrypted in future phases.
8. **VFS Path Traversal Prevention**: Future SAF integration must explicitly validate that file paths do not escape the allocated workspace root via malicious `../` patterns.

## AI Context Security Model
- **Data Boundary**: Xero intelligence providers (`XeroContextProvider`) operate strictly over explicit `ProjectContext` extraction. They are NOT granted direct file system manipulation or background database reads. 
- **AI Mutation Isolation**: All Xero outputs (`XeroAction`) are transformed into declarative `Command`s. No direct API integration is permitted to mutate project files.
- **Plugin Intelligence Permissions**: Any future plugin providing a `CodeAnalysisProvider` or `LanguageServiceProvider` executes within the `xIDE` app space. They do not have external API network capabilities by default and cannot exfiltrate symbol graphs.

## Execution Security Model
- **Command Abstraction**: All shell and terminal executions are routed through `TerminalProvider` and `AutomationCommand`. Direct, unsupervised shell access is strictly forbidden for the UI and AI layer.
- **Task Cancellation**: The `TaskQueue` enforces strict cancellation boundaries to prevent resource exhaustion from rogue processes.
- **Xero Task Sandbox**: Xero is limited to generating `XeroTaskResult` containing abstract commands. These commands must be explicitly approved or executed through the constrained `AutomationEngine`.

## Plugin Permission Architecture (Phase 7)
Permissions are granular capability strings (e.g., `terminal_execute`, `vfs_read`, `vfs_write`, `network_access`, `build_execute`).
- **Storage**: User grants are persisted securely via the application preferences or a secure database.
- **Flow**: On first initialization, xIDE prompts the user if requested permissions exceed previously granted ones.
- **Enforcement**: Interceptors at the `CommandSystem` and `EventBus` level verify the caller's identity (via CoroutineContext or ThreadLocal bounds) against the granted permissions.

## Xero / Plugin Security Model
- **Observation**: Xero can see active plugins and their exported `XideProvider` capabilities.
- **No Direct Execution**: Xero cannot call plugin code directly. It can only emit a `XeroTaskResult` proposing a standard `Command` that the plugin registered.
- **Command Constraints**: Plugin commands must implement `Command` or `AutomationCommand` and flow through the `CommandSystem`, which enforces permissions and undo-ability.

## Plugin Threat Model
- **Permission Escalation**: Defended by strict `CommandSystem` interception and explicit user prompts.
- **Filesystem Escape**: Defended by forcing all file operations through the Virtual File System (VFS) abstraction, which chroots to the workspace.
- **Command Injection**: Defended by requiring structured `TerminalCommand` objects rather than raw shell strings where possible, and strict `TerminalProvider` constraints.
- **Resource Exhaustion**: Defended by `AutomationEngine` limits (max concurrent tasks, timeouts) and `SupervisorJob` cancellation.

## Execution Sandbox Boundaries (Phase 8)
- `LocalTerminalProvider` will only execute commands sent explicitly through a verified session. The Xero agent cannot spawn or query a session directly.
- `TerminalOutputBuffer` limits all logging automatically.
- Tasks enqueued to `PersistentTaskQueue` are verified by intent (`CommandSystem` authorization layer mapping) before execution.

## Phase 10: Intelligence Model & Xero Action Security (New)
To ensure the live integration of Large Language Models (LLMs) remains completely secure, xIDE implements a multi-tiered security pipeline for all AI-generated proposals:
1. **No Direct Execution**: The LLM never has access to execution channels. It can only generate structured text representing desired actions.
2. **Structured Output Validation**: The `StructuredOutputValidator` intercepts raw model text, deserializing it strictly into an immutable `ActionPlan` containing `ActionDescriptor`s.
3. **Capability Whitelisting**: Every proposed `commandId` is validated against a static whitelist of safe capabilities (`automation.build_project`, `automation.run_terminal`, etc.). Unrecognized or hallucinated capabilities throw a `HallucinatedCapabilityException` and are immediately discarded.
4. **Command Injection Prevention**: High-risk arguments (e.g. terminal execution strings) are analyzed for malicious bash injection patterns (e.g., `rm -rf /`, `mkfs`, `chown`, `chmod`). Detection triggers an `UnsafeActionException` and halts the workflow.
5. **Provider Availability Validation**: Each validated command is checked against the set of active and healthy providers registered in the `ServiceRegistry`. Missing or degraded providers trigger an `UnavailableProviderException`.
6. **Explicit Permission Interception**: Proposed actions are checked against authorized plugin/agent permissions. Actions that exceed the granted scope (e.g. attempting writing files without `vfs_write`) trigger an `InvalidPermissionException` and are rejected before any user prompt.
7. **Human-in-the-Loop Gate**: All valid AI-proposed actions must be written to the `ApprovalManager` and explicitly confirmed in the UI by the user before execution.

## Plugin Sandboxing & OS-Level Boundaries (Remaining Limitations)
- **Current Plugin Security Model**: Plugins are registered in-process via the `LocalServiceRegistry` within the same JVM space as the main application.
- **What ServiceRegistry Protects**: The `ServiceRegistry` protects capability lookup and forces all capability access to verify registered permission claims. It enforces that capability IDs map only to registered, healthy, and authorized providers and checks their current health status and declared permission boundaries before executing actions.
- **What OS-Level Sandboxing Would Require Later**: Because all plugins currently run in the same JVM process and Android OS Application sandbox, rogue or malicious plugin code could bypass the `ServiceRegistry` via Java Reflection, direct file-system calls (`java.io.File`), or thread manipulation. A full, robust production-grade sandboxing model would require:
  1. **Isolated Processes**: Running each plugin in a separate Android service configured with `android:isolatedProcess="true"`.
  2. **IPC / Binder Communication**: Restricting interaction between the core IDE process and plugin processes exclusively through AIDL (Android Interface Definition Language) interfaces, where the main process enforces strict validation on all incoming payload formats and operations.
  3. **Unix UID Isolation**: Utilizing the OS-level isolated UID sandbox to strictly restrict filesystem access of the isolated plugin process to its own private directory, rendering reflection/chroot escapes impossible.


## Phase 11: AI Provider Abstraction and Data Trust Boundaries (New)
To safeguard user data and maintain strict platform-level privacy:
1. **Decoupled Trust Boundary**: The `AiService` and `AiContextManager` enforce a strict logical boundary between the raw workspace content (VFS, active problems, project metadata) and external AI API servers. No raw files are ever scanned directly by providers without passing through the controlled `AiContextSnapshot` serialization layer.
2. **Dynamic Capability Access Control**: The `AiCapabilityRouter` isolates model providers by routing requests according to narrow, declarative capability keys (`code_generation`, `error_analysis`). Model providers only receive the context explicitly packed within the specific routed request model.
3. **Strict Context Sanitation**: Before being transmitted across external network providers, all data formatted via `AiContextManagerImpl` must undergo strict sanitation, removing absolute local filesystem paths and system credentials.


## Phase 12: Automation Permission Boundary & Action Guards (New)
To enforce complete system integrity and prevent unsafe, rogue, or autonomous file and terminal executions:
1. **Path-Traversal Restriction**: The `AutomationPermissionVerifier` validates all filesystem paths against the active project's workspace directory. By resolving canonical paths, it guarantees that no action can traverse upwards (e.g., using `..` sequences) to touch, read, or modify sensitive system configurations.
2. **Execution Control Isolation**: Action models describe system intents (`AutomationAction`) but do not contain any execution capabilities. The actual operations remain entirely isolated within low-level, registered providers (`FileSystemAutomationProvider`, `TerminalAutomationProvider`) that can be safely audited and swapped out.
3. **Command Whitelisting**: Terminal executions are strictly gated. For security, only Gradle-specific toolchain commands (e.g., `gradlew`, `gradle`) are permitted during initial implementation. Arbitrary command-line strings are rejected immediately before reaching any shell execution layers.
4. **Human-in-the-Loop Action Approval**: All generated `AnalysisPlan` proposals flowing from Xero pass through a future approval manager, requiring explicit developer confirmation before they are sent to the `AutomationEngineImpl` for dispatch and execution.


## Phase 13: Build Execution Guards & Diagnostics Decoupling (New)
To guarantee that compilation tasks remain secure and never allow arbitrary payload injection:
1. **Command Parameterization**: Build execution parameters are strictly bound to predefined, whitelisted Gradle tasks (e.g., `assembleDebug`, `compileDebugKotlin`, `test`, `lint`). Ad-hoc CLI string injection or unvalidated option flags are completely rejected.
2. **Path Sanitization & Isolation**: `GradleBuildProvider` executes the wrapper only within the validated project root. Parent directory transitions are blocked, and classpath configurations are restricted to prevent remote code execution (RCE) via untrusted dependencies.
3. **Sandbox Execution Bounds**: Process builders are configured without inherited environment variables unless strictly necessary (such as `JAVA_HOME`), mitigating host credential leaks during compiler execution.
4. **Isolated Diagnostic Flow**: Logs are scanned and parsed strictly within a text-only regex environment. Unescaped terminal escape codes, ANSI control characters, or malicious command sequences inside compiler output are neutralized to prevent log injection exploits in both local files and AI context models.


## Phase 13.5: Build Pipeline Evidence Hardening (New)
To further isolate execution environments and protect against context leakage during AI-assisted debugging:
1. **Dynamic Whitelist Enforcement**: Task boundaries defined in `canBuild` are strictly enforced inside `executeBuild`. Any process spawning requests pointing to unsupported operations are rejected immediately with a `security_validation` diagnostic category, bypassing subprocess invocation completely.
2. **Scrubbing & Redaction Engine**: A dedicated real-time filter inside `DiagnosticsEngineImpl` scrubs any binary ANSI terminal color sequences and redacts highly sensitive credentials (e.g. passwords, authentication keys, tokens) with an immutable `[REDACTED_SECRET]` token.
3. **Reasoning Buffer Truncation**: A protective length threshold is placed on active diagnostic logs to limit individual diagnostic messages to 1000 characters. This prevents malicious files from intentionally failing compile processes and injecting enormous data payloads to overflow, hijack, or exhaust the AI reasoning window.




