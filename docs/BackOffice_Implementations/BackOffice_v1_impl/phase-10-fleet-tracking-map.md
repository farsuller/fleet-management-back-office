# Phase 10 — Fleet Tracking Map

> **Status**: `COMPLETED`
> **Prerequisite**: Phase 1 (`FleetLiveClient`) + Phase 9
> [← Back to master plan](../web-backoffice-implementation-plan.md)

**Goal**: real-time OSM tile map with WebSocket live vehicle positions and animated markers.
> Implementation details: [`docs/live-tracking-feature.md`](../live-tracking-feature.md)

> **Note**: Implemented as a Compose Multiplatform canvas map (OSM tiles via Coil 3 + Web Mercator projection) rather than an SVG canvas. The architecture is equivalent — same projection math, same WebSocket state flow, same animation tween — but rendered with Compose `Box`/`Canvas` instead of raw SVG elements.

---

## Core Utilities

- [x] `SvgUtils.wktToPoints(lineString)` — converts PostGIS WKT LineString → `List<Pair<Double,Double>>`
- [x] `MapProjection.toCanvasXY(lat, lon, mapState, w, h)` — Web Mercator (EPSG:3857) → canvas pixel coordinate
- [x] `MapViewState` — immutable viewport state (`centerLat`, `centerLon`, `zoom`); `zoomedIn()`, `zoomedOut()`, `panned(dx, dy)` all return copies; `MIN_ZOOM = 3`, `MAX_ZOOM = 18`
- [x] `FleetState` — `MutableStateFlow<Map<VehicleId, VehicleRouteState>>` wired to `FleetLiveClient`

## Map Canvas (`FleetMapCanvas`)

- [x] **Fixed-lens viewport**: outer `Box(clipToBounds)` as the visible lens + inner `Box(requiredSize(1920.dp, 1080.dp))` as the fixed projection canvas — decouples layout from projection so zoom never jumps
- [x] **OSM tile layer** (`OsmTileLayer`): Coil 3 `AsyncImage` per tile; horizontal X wrapping `((tx % n) + n) % n`; no API key required (free/open-source)
- [x] **Route polylines**: `Canvas` layer draws WKT routes as `Path` strokes via `MapProjection.toCanvasXY`
- [x] **Car icon markers** (Layer 3): composable `Image` layer using `painterResource(Res.drawable.car_top)`; each vehicle `absoluteOffset`-positioned and `Modifier.rotate(headingDeg)`-rotated; 500ms `animateFloatAsState` tween on lat/lon/rotation
- [x] **Selected vehicle**: 44dp icon with translucent circle ring + 2dp primary-colour border; unselected is 32dp
- [x] **Drag-to-pan**: `detectDragGestures` on lens box; `MapViewState.panned(dx, dy)` uses inverse Web Mercator math to convert pixel delta → new centre lat/lon; hand cursor via `pointerHoverIcon`
- [x] **Zoom controls**: `+`/`−` `IconButton`s; buttons disabled and dimmed at `MIN_ZOOM`/`MAX_ZOOM` limits
- [x] Right sidebar: vehicle list sorted by status, click → highlight + select vehicle marker
- [x] Selection info panel: plate, speed, heading, route progress %, last update timestamp
- [x] **OSM attribution**: `© OpenStreetMap contributors` pinned to bottom-right of outer lens box (always visible)
- [x] Status bar: `ConnectionState` indicator — Connected (green) / Reconnecting (amber pulse) / Offline (red)

## WebSocket Connection Rules

- [x] `CONNECTING` state on connect; `Authorization: Bearer {token}` in header
- [x] Heartbeat: Ping every 30s; disconnect on Pong timeout (30s)
- [x] Auto-reconnect: fixed 5s delay on any disconnect or error
- [x] `disconnect()` called cleanly on page unload

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
| `GET /v1/tracking/routes` (route geometry) | 30 s TTL in `routesCache` | Tile + polyline data is expensive; geometry rarely changes mid-session |
| `GET /v1/tracking/fleet/status` | 30 s TTL in `statusCache` | Aggregate counts; supplement real-time WS data |
| `GET /v1/tracking/{id}/history` | **Not cached** | Raw historical reads are explicit user-initiated queries |
| `GET /v1/tracking/{id}/state` | **Not cached** | Point-in-time snapshot; superseded by WS delta within seconds |
| WebSocket `VehicleStateDelta` | `FleetState: MutableStateFlow<Map<VehicleId, VehicleRouteState>>` | In-memory live state; not stored in `InMemoryCache` — updated on every WS frame |

**On map mount**: call `getActiveRoutes(forceRefresh = false)` followed by `FleetLiveClient.connect()`. Routes are rendered immediately from cache if warm; the WS stream then continuously patches vehicle positions.

**On explicit user refresh**: `getActiveRoutes(forceRefresh = true)` + `getFleetStatus(forceRefresh = true)` — reloads geometry and aggregate counts; WS connection is unaffected.

## Use Cases (Clean Architecture — Phase 1.11)

- [x] `GetFleetStatusUseCase(trackingRepository)` — delegates to `trackingRepository.getFleetStatus(forceRefresh)`
- [x] `GetActiveRoutesUseCase(trackingRepository)` — delegates to `trackingRepository.getActiveRoutes(forceRefresh)`
- [x] `GetVehicleStateUseCase(trackingRepository)` — delegates to `trackingRepository.getVehicleState(id)`
- [x] Registered in `UseCaseModule.kt`; injected into `FleetTrackingViewModel`
- [x] `fleetLiveClient` (WebSocket) injected **directly** into `FleetTrackingViewModel` — infrastructure, not a domain use case

## Additional Improvements (Post-Plan)

- [x] **CORS fix**: backend `Application.kt` allows `localhost:8082` and `127.0.0.1:8082`
- [x] **GeoJSON import**: `ImportRouteDialog` + `vm.importRoute()` — paste GeoJSON from geojson.io, saves route to backend, reloads map
- [x] **Import button icon**: custom `json_icon.png` resource in `composeResources/drawable/`
- [x] **Car icon markers**: top-down `car_top.png` replaces placeholder triangles; pointer-rotated to heading, click-selectable

## Verification

- [x] OSM tiles render correctly at all zoom levels (3–18)
- [x] Routes render as polylines projected onto the OSM tile viewport
- [x] Vehicles animate with 500ms tween on each position update
- [x] Car icon rotates to match vehicle heading
- [x] WebSocket reconnects automatically after 5s on disconnect
- [x] Ping/Pong heartbeat active; disconnects on pong timeout
- [x] Sidebar click highlights the correct vehicle marker
- [x] `ConnectionState` indicator visible in status bar
- [x] Drag-to-pan moves the map; hand cursor shown on hover
- [x] Zoom buttons disabled at min/max limits
- [x] OSM attribution always visible in bottom-right corner
- [x] Routes load from cache on warm revisit
- [x] WS delta updates `FleetState` within one frame of receipt
