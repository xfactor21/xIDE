# Plugin Engine Architecture (Phase 7)

## Plugin Lifecycle
1. **DISCOVERED**: Plugin manifest found on the VFS.
2. **VALIDATING**: Checking compatibility, dependencies, and manifest integrity.
3. **INSTALLED**: Validated and stored in the local registry, awaiting user activation.
4. **ENABLED**: Marked for execution but not yet loaded.
5. **INITIALIZING**: Classloader instantiated, `XideProvider`s instantiated, `initialize()` called.
6. **ACTIVE**: All providers registered and healthy.
7. **DISABLED**: Gracefully shut down (`shutdown()`), providers unregistered.
8. **FAILED**: Encountered uncaught exception, health check failed, or initialization timeout. Providers unregistered, supervisor job cancelled.
9. **REMOVED**: Uninstalled from the system.

## Plugin Manifest (`xide-plugin.json`)
- **Identity**: `pluginId`, `pluginName`, `author`, `version`, `description`.
- **Compatibility**: `requiredXideVersion`, `compatibilityRange`.
- **Runtime**: `entryPoint` (fully qualified class name), `exportedProviders` (list of `XideProvider` implementations).
- **Permissions**: `requestedCapabilities` (e.g., `vfs_write`, `terminal_execute`).
- **Dependencies**: `requiredPlugins` (list of pluginIds and minimum versions).

## Discovery Architecture
Plugins are discovered by scanning a designated Virtual File System (VFS) partition (e.g., `workspace/.xide/plugins/` or `system/plugins/`).
1. **Locate**: Scan for `xide-plugin.json` or `.jar`/`.dex` bundles.
2. **Read Manifest**: Parse metadata before loading any classes.
3. **Validate Metadata**: Ensure required fields exist.
4. **Validate Compatibility**: Check `requiredXideVersion` against current IDE version.
5. **Validate Permissions**: Cross-reference requested permissions with user-granted permissions.
6. **Load Plugin**: Create an isolated ClassLoader (if JVM based).
7. **Initialize Providers**: Instantiate `entryPoint` and call `initialize()`.

## Version and Dependency Model
- **Semantic Versioning**: Standard MAJOR.MINOR.PATCH rules apply.
- **Dependency Resolution**: Plugins declare dependencies. xIDE builds a DAG (Directed Acyclic Graph) to determine initialization order.
- **Conflict Handling**: If Plugin A needs CompilerProvider v2 and Plugin B needs CompilerProvider v3: xIDE enforces a strict "one active provider per capability/language" rule at the ServiceRegistry level, or supports side-by-side ClassLoaders if strictly isolated. In Phase 7, highest compatible version wins, or explicit user resolution is required.
- **Upgrades/Downgrades**: Trigger a `DISABLED` -> `VALIDATING` -> `INITIALIZING` flow.

## Future Execution Providers
Plugins will contribute to the automation ecosystem via:
- **BuildProvider**: Integration with custom build tools (e.g., Gradle, Bazel, Webpack).
- **CompilerProvider**: Integration with external compilation binaries.
- **PackageManagerProvider**: Extensions to support alternate repositories (e.g., NPM, Maven, PyPI).

## Plugin Runtime Loading (Phase 8)
- The `PluginRuntimeLoader` creates individual `PluginSupervisor` instances to load providers.
- Provider instances are submitted to the `LocalServiceRegistry`.
- `ProviderHealthMonitor` checks the provider's heartbeat asynchronously. If a plugin throws, the monitor flags it as `DEGRADED` or `FAILING` and it is bypassed during `ServiceRegistry.resolve()`.
