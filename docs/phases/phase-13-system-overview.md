# Phase 13 — System Overview (Onboarding Demo)

> **Status**: `NOT STARTED`
> **Prerequisite**: Phase 2 (auth session needed to resolve logged-in user role)
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: A scrollable `/system-overview` screen that serves as an interactive onboarding demo — explaining the complete Fleet Management architecture to new staff members, technical stakeholders, or during a live product demo. The page is divided into four visually distinct sections rendered entirely in Compose, with no external dependencies.

---

## Layout Overview

```
┌─────────────────────────────────────────────────────────┐
│  HERO                                                   │
│  "Fleet Management System"  ·  tagline  ·  3 badges    │
├─────────────────────────────────────────────────────────┤
│  SECTION 1 — System Architecture Diagram                │
│  Canvas overlay (connector lines) + positioned nodes    │
│  Clients → API → Modules → DB / Cache                  │
├─────────────────────────────────────────────────────────┤
│  SECTION 2 — Backoffice Architecture                    │
│  KMP / Compose Multiplatform stack  ·  layer diagram    │
├─────────────────────────────────────────────────────────┤
│  SECTION 3 — Deployment                                 │
│  Render.com  ·  Supabase  ·  CI/CD pipeline            │
├─────────────────────────────────────────────────────────┤
│  SECTION 4 — Mobile Apps                               │
│  2-column: Driver App  |  Customer App                 │
│  Feature list + screen mockup cards                    │
└─────────────────────────────────────────────────────────┘
```

---

## Route & Navigation

- Route: `Screen.SystemOverview` (`/system-overview`)
- Sidebar item: "System Overview" with `Icons.Outlined.AccountTree`
- Visible to **all authenticated roles** (no guard needed — read-only informational page)
- `LazyColumn` root with `StickyHeader` per section

---

## 13.1 Hero Section

```
┌─────────────────────────────────────────────────────────┐
│  Fleet Management System                                │
│  A full-stack vehicle rental & operations platform      │
│                                                         │
│  [Ktor 3]  [Compose Multiplatform]  [PostgreSQL+PostGIS]│
└─────────────────────────────────────────────────────────┘
```

### Deliverables
- [ ] `HeroSection` composable — large title + subtitle + tech badge row
- [ ] `TechBadge(label, icon)` — pill-shaped chip with icon, used across all sections
- [ ] Static badges: `Ktor 3`, `Kotlin Multiplatform`, `Compose WASM`, `PostgreSQL + PostGIS`, `Render.com`, `Supabase`, `JUnit 5 + MockK`

---

## 13.2 System Architecture Diagram

An interactive node-and-connector diagram rendered entirely in Compose using **`Canvas` for lines** and **`Box` with `absoluteOffset` for node cards** — no JS/SVG libraries needed in WASM.

### Diagram layout (logical)

```
  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
  │ Web Backoffice│    │  Driver App  │    │ Customer App  │
  │  (WASM/KMP)  │    │ (Android/KMP)│    │(Android/KMP) │
  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │  HTTPS + JWT
                    ┌────────▼────────┐
                    │  Ktor 3 Backend │
                    │  REST API + WS  │
                    │  (Render.com)   │
                    └────────┬────────┘
           ┌─────────────────┼─────────────────┐
           │                 │                 │
    ┌──────▼──────┐  ┌───────▼──────┐  ┌──────▼──────┐
    │   Rentals   │  │   Vehicles   │  │   Customers │
    │  + Invoicing│  │  + GPS Track │  │  + Drivers  │
    └──────┬──────┘  └───────┬──────┘  └──────┬──────┘
           │                 │                 │
    ┌──────▼──────┐  ┌───────▼──────┐
    │ Maintenance │  │  Accounting  │
    │  + Parts    │  │  Double-Entry│
    └──────┬──────┘  └───────┬──────┘
           │                 │
           └────────┬────────┘
                    │
         ┌──────────▼──────────┐
         │   PostgreSQL        │
         │   + PostGIS         │
         │   (Supabase)        │
         └──────────┬──────────┘
                    │
         ┌──────────▼──────────┐
         │   Redis Cache       │
         │   Pub/Sub (WS)      │
         └─────────────────────┘
```

### DiagramNode data model

```kotlin
data class DiagramNode(
    val id: String,
    val label: String,
    val sublabel: String = "",
    val tier: DiagramTier,           // CLIENT | API | MODULE | STORAGE
    val color: Color,
)

enum class DiagramTier { CLIENT, API, MODULE, STORAGE }
```

### Composables

```kotlin
@Composable
fun SystemArchitectureDiagram(modifier: Modifier = Modifier)

@Composable
private fun ArchDiagramCanvas(
    nodes: List<PlacedNode>,          // node + resolved pixel offset
    connections: List<Pair<String, String>>,
    modifier: Modifier,
)

@Composable
private fun DiagramNodeCard(node: DiagramNode, modifier: Modifier)
```

**Connection rendering**: `DrawScope.drawLine` with `strokeWidth = 1.5.dp.toPx()`, `color = onBackground.copy(alpha = 0.25f)`, capped with a small arrowhead triangle at the target end.

**Responsive**: On narrow viewports (`LocalWindowInfo < 800.dp`) the diagram collapses to a vertical list of tier-labelled rows instead of the free-form canvas layout.

### Deliverables
- [ ] `DiagramNode` data class + `DiagramTier` enum
- [ ] `SystemArchitectureDiagram` composable — `Box` root with backing `Canvas` for connectors + overlaid node cards
- [ ] `DiagramNodeCard(node, modifier)` — `Card` with tier-accent left border, label, sublabel, `TechBadge` chips
- [ ] Connector lines drawn in `DrawScope` with arrowhead triangles
- [ ] Narrow-viewport fallback (tier rows)
- [ ] Section header: "System Architecture" with subtitle "How all the pieces connect"

---

## 13.3 Backoffice Architecture Section

A layered stack diagram (top-to-bottom) showing the KMP web app Clean Architecture.

```
  ┌─────────────────────────────────────────┐
  │  Presentation Layer                     │
  │  Compose WASM · Screens · ViewModels   │
  ├─────────────────────────────────────────┤
  │  Domain Layer (Use Cases)               │
  │  AuthUseCases · VehicleUseCases         │
  │  RentalUseCases · GetDashboardUseCase   │
  ├─────────────────────────────────────────┤
  │  Data Layer (Repositories)              │
  │  Repository interfaces · InMemoryCache  │
  ├─────────────────────────────────────────┤
  │  Infrastructure Layer                   │
  │  FleetApiClient (Ktor) · DTOs           │
  │  WebSocket (FleetLiveClient) · KSafe    │
  └─────────────────────────────────────────┘
         ▼ compiled to WebAssembly (WASM)
```

> **Dependency rule**: each layer only imports the layer directly below it. ViewModels → use cases → repository interfaces → API client. No layer skips a level.

### Deliverables
- [ ] `BackofficeArchitectureSection` composable — stacked `LayerCard` rows with gradient tier colours
- [ ] `LayerCard(title, subtitle, chips)` — full-width card with accent colour per layer
- [ ] Tech highlights per layer as `TechBadge` chips:
  - **Presentation**: `Compose WASM`, `ViewModels`, `Koin`
  - **Domain (Use Cases)**: `Clean Architecture`, `Single-Responsibility`, `no framework imports`
  - **Data (Repositories)**: `InMemoryCache`, `TTL`, `forceRefresh`
  - **Infrastructure**: `Ktor 3`, `WebSocket`, `KSafe AES-256-GCM`
- [ ] Downward arrow between layers (Compose `Canvas` small chevron)
- [ ] "Compiled to WebAssembly" footer badge row
- [ ] Section header: "Backoffice Architecture" with subtitle "Kotlin Multiplatform web app — Clean Architecture"

---

## 13.4 Deployment Section

```
  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
  │ GitHub Repo  │────▶│ Render.com   │────▶│   Supabase   │
  │ CI/CD push   │     │ Docker image │     │  PostgreSQL  │
  │ main branch  │     │ Auto-deploy  │     │  + PostGIS   │
  └──────────────┘     └──────┬───────┘     └──────────────┘
                              │
                     ┌────────▼────────┐
                     │  api.solodev    │
                     │  .fleet.com     │
                     │  (HTTPS / JWT)  │
                     └─────────────────┘
```

### Deliverables
- [ ] `DeploymentSection` composable — horizontal `Row` of step cards connected by arrows on wider screens, vertical stack on narrow
- [ ] `DeployStepCard(icon, title, lines)` — icon + title + bullet lines
- [ ] Steps: **GitHub** → **Render.com (Docker)** → **Supabase** with connecting arrow chips
- [ ] Highlights: zero-downtime deploys, free-tier cold starts, auto-SSL
- [ ] Section header: "Deployment" with subtitle "How the system is hosted and shipped"

---

## 13.5 Mobile Apps Section

Two-column card layout. Each card lists the target role, key features, and permission requirements.

```
  ┌─────────────────────────┬─────────────────────────┐
  │  Driver App             │  Customer App           │
  │  Role: DRIVER           │  Role: CUSTOMER         │
  │                         │                         │
  │  ● Live GPS telemetry   │  ● Browse vehicle catalog│
  │  ● Shift management     │  ● Book rentals         │
  │  ● Work hours tracking  │  ● Track active rental  │
  │  ● Geofence alerts      │  ● View invoices        │
  │  ● Odometer submission  │  ● Payment history      │
  │                         │                         │
  │  Permissions required:  │  Permissions required:  │
  │  ACCESS_FINE_LOCATION   │  INTERNET               │
  │  FOREGROUND_SERVICE     │                         │
  │  BACKGROUND_LOCATION    │                         │
  └─────────────────────────┴─────────────────────────┘
```

### Deliverables
- [ ] `MobileAppsSection` composable — `Row` (wide) / `Column` (narrow) of two `AppProfileCard`s
- [ ] `AppProfileCard(role, accentColor, features, permissions)` — card with role badge at top, feature bullet list, permission chip row at bottom
- [ ] `FeatureBullet(text)` — `Icon(CheckCircle) + Text` row
- [ ] `PermissionChip(name)` — outlined chip with lock icon
- [ ] Section header: "Mobile Apps" with subtitle "Compose Multiplatform Android — same codebase, role-switched UI"

---

## Shared Composables (new in this phase)

| Composable | Location | Reused by |
|---|---|---|
| `TechBadge(label, icon?)` | `components/overview/` | Hero, Architecture, Deployment |
| `SectionHeader(title, subtitle)` | `components/overview/` | All 4 sections |
| `DiagramNodeCard(node)` | `components/overview/` | System Architecture diagram |
| `LayerCard(title, subtitle, chips)` | `components/overview/` | Backoffice Architecture |
| `DeployStepCard(icon, title, lines)` | `components/overview/` | Deployment |
| `AppProfileCard(...)` | `components/overview/` | Mobile Apps |

All composables live in `features/overview/` (screen + ViewModel) and `components/overview/` (reusable UI).

---

## ViewModel

No API calls — all data is static. The ViewModel holds no `StateFlow` of remote data.

```kotlin
class SystemOverviewViewModel : ViewModel() {
    val architectureNodes: List<DiagramNode> = buildArchitectureNodes()
    val connections: List<Pair<String, String>> = buildConnections()
    // backoffice layers, deployment steps, mobile app cards — all pure data
}
```

---

## Caching Behavior

None — this screen is entirely static. No repository, no network calls, no `InMemoryCache`.

---

## Navigation Integration

```kotlin
// In Screen.kt — add entry
object SystemOverview : Screen("system-overview")

// In AppShell.kt — add sidebar item (all roles)
NavSidebarItem(
    item = NavItem("System Overview", Icons.Outlined.AccountTree, Screen.SystemOverview),
    selected = currentScreen == Screen.SystemOverview,
    onClick = { navTo(Screen.SystemOverview) },
)

// In AppRouter — add composable
composable(Screen.SystemOverview.route) {
    SystemOverviewScreen(viewModel = koinViewModel())
}
```

---

## Verification

- [ ] `/system-overview` is accessible from sidebar for all authenticated roles
- [ ] All 4 sections render without errors on wide (≥ 1200 dp) and narrow (< 800 dp) viewports
- [ ] Connector lines in the architecture diagram connect the correct nodes visually
- [ ] Narrow viewport: diagram collapses to tier-row fallback (no overflow/clipping)
- [ ] `TechBadge`, `SectionHeader` composables are reused across all 4 sections (no duplication)
- [ ] No network calls made from this screen (`0` Ktor requests in dev tools)
- [ ] Page scrolls smoothly end-to-end with no frame drops (Compose profiler)
