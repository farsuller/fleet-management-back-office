# Phase 9 — Fleet Tracking Map

> **Status**: `NOT STARTED`
> **Prerequisite**: Phase 1 (`FleetLiveClient`) + Phase 3
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: real-time SVG fleet map with WebSocket live vehicle positions and animated markers.
> See also: [`docs/web-schematic-visualization.md`](../web-schematic-visualization.md) for full SVG architecture.

---

## Core Utilities

- [ ] `SvgUtils.polylineToPath(lineString)` — converts PostGIS LineString → SVG `<path>` data
- [ ] `SvgUtils.getPointAtProgress(lineString, progress)` — returns `Point(x, y)` at 0.0..1.0 along polyline
- [ ] `DeltaDecoder.merge(current, delta)` — partial updates overwrite; absent fields retain current values
- [ ] `FleetState` — `MutableStateFlow<Map<VehicleId, VehicleRouteState>>` wired to `FleetLiveClient`

## SVG Canvas (`FleetMap`)

- [ ] Root `<svg viewBox="0 0 1000 1000">` with `#0F172A` background
- [ ] `RouteLayer`: one `<path>` per route from `GET /v1/tracking/routes`, `stroke: #334155`, `fill: none`
- [ ] `VehicleIcon`: `<polygon>` directional marker, status token color, 500ms `animateFloatAsState` tween, `rotate(bearing)` transform
- [ ] Right sidebar: vehicle list sorted by status, click → highlight + center map on marker
- [ ] Selection info panel: plate, speed, heading, route progress %, last update timestamp
- [ ] Status bar: `ConnectionState` indicator — Connected (green) / Reconnecting (amber pulse) / Offline (red)

## WebSocket Connection Rules

- [ ] `CONNECTING` state on connect; `Authorization: Bearer {token}` in header
- [ ] Heartbeat: Ping every 30s; disconnect on Pong timeout (30s)
- [ ] Auto-reconnect: fixed 5s delay on any disconnect or error
- [ ] `disconnect()` called cleanly on page unload

## `FrontendMetrics`

- [ ] `recordRenderTime(vehicleId, durationMs)` → `window.performance`
- [ ] `recordWebSocketLatency(latencyMs)` → `window.performance`
- [ ] `recordAnimationFps(fps)` → `window.performance`
- [ ] All metrics calls stripped from production build via `BuildConfig` flag

## Performance Targets

| Metric | Target | Critical threshold |
|---|---|---|
| FPS (50 vehicles) | ≥ 55 fps | < 30 fps |
| Render time per frame | < 16ms | > 33ms |
| WS message → render | < 100ms | > 500ms |
| Time to first render | < 2s | > 5s |
| Memory usage | < 100MB | > 500MB |

## Caching Behavior

> `TrackingRepositoryImpl` · fleet status TTL **30 s** · active routes TTL **30 s** · real-time positions via WebSocket (not cached).

| Data source | Cache | Rationale |
|---|---|---|
| `GET /v1/tracking/routes` (route geometry) | 30 s TTL in `routesCache` | SVG path data is expensive to render; geometry rarely changes mid-session |
| `GET /v1/tracking/fleet/status` | 30 s TTL in `statusCache` | Aggregate counts; supplement real-time WS data |
| `GET /v1/tracking/{id}/history` | **Not cached** | Raw historical reads are explicit user-initiated queries |
| `GET /v1/tracking/{id}/state` | **Not cached** | Point-in-time snapshot; superceded by WS delta within seconds |
| WebSocket `VehicleStateDelta` | `FleetState: MutableStateFlow<Map<VehicleId, VehicleRouteState>>` | In-memory live state; not stored in `InMemoryCache` — updated on every WS frame |

**On map mount**: call `getActiveRoutes(forceRefresh = false)` followed by `FleetLiveClient.connect()`. Routes are rendered immediately from cache if warm; the WS stream then continuously patches vehicle positions.

**On explicit user refresh**: `getActiveRoutes(forceRefresh = true)` + `getFleetStatus(forceRefresh = true)` — reloads geometry and aggregate counts; WS connection is unaffected.

## Use Cases (Clean Architecture — Phase 1.11)

> Create these in `domain/usecase/tracking/TrackingUseCases.kt` **before** building `TrackingViewModel`. See Phase 1.11 for the full checklist.

- [ ] `GetFleetStatusUseCase(trackingRepository)` — delegates to `trackingRepository.getFleetStatus(forceRefresh)`
- [ ] `GetActiveRoutesUseCase(trackingRepository)` — delegates to `trackingRepository.getActiveRoutes(forceRefresh)`
- [ ] `GetVehicleStateUseCase(trackingRepository)` — delegates to `trackingRepository.getVehicleState(id)`
- [ ] Register all 3 in `UseCaseModule.kt`; inject into `TrackingViewModel`
- [ ] Note: `fleetLiveClient` (WebSocket) is injected **directly** into `TrackingViewModel` — it is infrastructure, not a domain use case

## Verification

- [ ] Routes render as SVG paths from `GET /v1/tracking/routes`
- [ ] Vehicles animate with 500ms tween on each position update
- [ ] WebSocket reconnects automatically after 5s on disconnect
- [ ] Ping/Pong heartbeat active; disconnects on pong timeout
- [ ] Sidebar click highlights and centers the correct vehicle marker
- [ ] `ConnectionState` indicator visible in status bar
- [ ] Routes load from cache on warm revisit (DevTools shows 0 GET requests to `/v1/tracking/routes`)
- [ ] WS delta updates `FleetState` within one frame of receipt
