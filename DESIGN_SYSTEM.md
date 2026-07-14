# xIDE Design System

This document outlines the Design Token Architecture for xIDE.
Implementation of concrete themes will proceed in later phases.

## Core Design Tokens
Tokens are semantic variables used instead of hardcoded values.

### Colors
- `primary`, `onPrimary`, `primaryContainer`
- `secondary`, `surface`, `background`, `error`
- `xeroBrand`: Specific palette dedicated to the Xero agent states (idle, thinking, alert).

### Typography
- `displayLarge`, `bodyMedium`, `labelSmall`
- Code font specifics (e.g., `JetBrains Mono` or similar monospaced font).

### Spacing & Layout
- Mobile-first adaptive padding margins (4dp, 8dp, 16dp, 24dp).

### Elevation
- Defined shadow and z-index layers for floating components, bottom sheets, and the Xero floating action unit.

### Motion & Animation
- `durationFast`, `durationNormal`, `durationSlow`.
- Easing curves for Xero expressions, sheet dismissals, and command executions.

## Editor UI Components (Phase 4B)
- **EditorViewport**: Utilizes `TypographyTokens` with `FontFamily.Monospace` for precise code alignment.
- **EditorTabs**: Leverages `MaterialTheme.colorScheme.surfaceVariant` for inactive tabs and `surface` for active ones. Uses standard `SpacingTokens` for touch targets.
- **EditorToolbar**: Utilizes standard `XideButton` and `XidePanel` components to maintain aesthetic consistency with the Application Shell.
