# Live Fleet Tracking — Schematic Map Feature

> **Comprehensive implementation guide — Frontend to Backend**

---

## Overview

The **Live Fleet Tracking** screen is one of the most technically demanding features in the Fleet Management Back-Office. It renders a real-time, interactive map of the Metro Manila road network using **OpenStreetMap (OSM)** raster tiles, overlays all active vehicle routes as polylines, and shows animated vehicle markers that update their position, speed, and heading in real time via a persistent **WebSocket** connection.

> **OpenStreetMap is 100% free and open-source.**
> Tiles are served from `tile.openstreetmap.org` at no cost, with no account, API key, paid plan, or usage quota required.
> The only obligation is attribution — the "© OpenStreetMap contributors" notice shown in the map corner, as required by the [ODbL licence](https://www.openstreetmap.org/copyright).

```
┌─────────────────────────────────────────────────────────────────┐
│              Live Fleet Tracking Screen                         │
│                                                                 │
│  ┌──────────────────────────────────┐  ┌──────────────────────┐│
│  │                                  │  │  Vehicle Sidebar     ││
│  │     OSM Tile Layer (bg)          │  │  ─────────────────── ││
│  │   + Canvas Overlay               │  │  🟢 VH-001  45 km/h  ││
│  │     • Route polylines            │  │  🟢 VH-002  32 km/h  ││
│  │     • Animated vehicle markers   │  │  ⚪ VH-003  Offline  ││
│  │                                  │  │  ─────────────────── ││
│  │  [↺] [+] [−] [↑] ← control btns │  │  [Selected Vehicle]  ││
│  └──────────────────────────────────┘  └──────────────────────┘│
│  ─────────────────────────────────────────────────────────────  │
│  🟢 WebSocket Connected — Streaming live updates                │
└─────────────────────────────────────────────────────────────────┘
```

### Key Capabilities

| Capability | Detail |
|---|---|
| **OSM Tile Map** | Free, open-source Slippy Map tiles from `tile.openstreetmap.org` — no API key, no plan, no cost. Web Mercator (EPSG:3857) projection |
| **Route Polylines** | WKT `LINESTRING` geometries stored in PostGIS, rendered as canvas `Path` overlays |
| **Live Vehicle Markers** | WebSocket delta stream from Redis pub/sub; animated triangle markers with `headingDeg` rotation |
| **Zoom Controls** | Manual zoom in/out (levels 3–18, buttons disabled at limits) + auto-fit bbox to all loaded routes |
| **Drag-to-Pan** | Click-and-drag the map to pan in any direction; hand cursor on hover; inverse Web Mercator math converts pixel delta to new centre lat/lon |
| **Horizontal Tile Wrapping** | Tile X indices wrap modulo `2^zoom` so the map is seamless across the date line at all zoom levels |
| **Fixed-Lens Viewport** | Internal canvas is always 1920×1080 dp (stable projection), clipped by the real container — no zoom jump artefacts |
| **Route Import** | Draw route on [geojson.io](https://geojson.io), paste GeoJSON, save to PostGIS via REST |
| **Sidebar** | Per-vehicle status: live dot, speed, route progress; detail panel on selection |
| **Resilience** | Rate limiting (60/min), idempotency keys, circuit breaker on POST /location |

---

## Architecture — Full Stack Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐  
│                          Kotlin/WASM Browser App                                │
│                                                                                 │
│  LiveTrackingScreen (Compose)                                                   │
│       │                                                                         │
│       ├─── FleetTrackingViewModel (StateFlow hub)                               │
│       │         │                                                               │
│       │         ├─── GetActiveRoutesUseCase ──────► TrackingRepository          │
│       │         │                                        │                      │
│       │         ├─── GetFleetStatusUseCase ──────────────┤                      │
│       │         │                                        │                      │
│       │         ├─── TrackingRepository.createRoute() ───┤                      │
│       │         │                                        │                      │
│       │         └─── FleetLiveClient (WebSocket) ─────── ┤                      │
│       │                    │ VehicleStateDelta                                  │
│       │                    └── DeltaDecoder.merge() ──► fleetState Map          │
│       │                                                                         │
│       ├─── FleetMapCanvas                                                       │
│       │         ├─── OsmTileLayer ── [AsyncImage per tile via Coil 3]           │
│       │         └─── Canvas Overlay                                             │
│       │                   ├─── MapProjection.toCanvasXY()                       │
│       │                   ├─── SvgUtils.wktToPoints() → route polylines         │
│       │                   └─── drawVehicleMarker() → animated triangles         │
│       │                                                                         │
│       └─── ImportRouteDialog → POST /v1/tracking/routes                         │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │ HTTPS / WSS
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               Ktor Backend                                      │
│                                                                                 │
│  TrackingRoutes.kt                        TrackingRoutes.kt                     │
│  GET  /v1/tracking/routes/active          POST /v1/tracking/routes              │
│  GET  /v1/tracking/fleet/status           POST /v1/tracking/vehicles/{id}/loc.  │
│  WS   /v1/fleet/live                                                            │
│       │                                        │                                │
│       └─── PostGISAdapter ─────────────────────┘                               │
│                 ├── findAllRoutes() → ST_AsText(polyline)                       │
│                 ├── createRoute() → PGgeometry insert                           │
│                 └── snapToRoute() / isInsideGeofence()                          │
│                                                                                 │
│  RedisDeltaBroadcaster ──► WebSocket /v1/fleet/live → VehicleStateDelta JSON   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PostgreSQL 15 + PostGIS                               │
│                                                                                 │
│  routes       (id UUID, name, polyline GEOMETRY(LineString,4326))               │
│  geofences    (id UUID, name, type, boundary GEOMETRY(Polygon,4326))            │
│  location_history (vehicle_id, latitude, longitude, progress, speed, heading…) │
│  vehicles     (+ last_location GEOMETRY(Point,4326), route_progress, bearing)  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack & Dependencies

### Frontend (Kotlin/WASM Compose Multiplatform)

| Library | Version | Purpose |
|---|---|---|
| `io.coil-kt.coil3:coil-compose` | 3.1.0 | Async tile image loading (`AsyncImage`) |
| `io.coil-kt.coil3:coil-network-ktor3` | 3.1.0 | Coil HTTP engine backed by Ktor |
| `io.ktor:ktor-client-core` | 3.1.2 | HTTP REST client + WebSocket engine |
| `io.ktor:ktor-client-websockets` | 3.1.2 | WebSocket frame handling in `FleetLiveClient` |
| `org.jetbrains.kotlinx:kotlinx-coroutines` | 1.10.1 | `StateFlow`, `SharedFlow`, `CoroutineScope`, `SupervisorJob` |
| `org.jetbrains.kotlinx:kotlinx-serialization` | 1.8.0 | JSON DTO deserialization (`@Serializable`) |
| `org.jetbrains.kotlinx:kotlinx-datetime` | 0.7.1 | Timestamp handling in DTOs |
| `io.insert-koin:koin-compose` | (project) | `koinViewModel<>()` DI for `FleetTrackingViewModel` |
| Compose `Canvas`, `animateFloatAsState` | multiplatform | Route/marker drawing, smooth animations |
| Compose `detectDragGestures`, `pointerHoverIcon` | multiplatform | Drag-to-pan gesture + hand cursor on the map |
| Compose `Box`, `clipToBounds`, `requiredSize` | multiplatform | Fixed-lens viewport — stable 1920×1080 inner canvas clipped by real container |

### Backend (Ktor/JVM)

| Library | Purpose |
|---|---|
| `io.ktor:ktor-server-*` | HTTP routing, WebSocket server, JWT auth |
| `org.jetbrains.exposed:exposed-core/jdbc` | Kotlin DSL ORM over PostgreSQL |
| `org.postgresql:postgresql` | JDBC driver |
| `net.postgis:postgis-jdbc` | `PGgeometry` type for WKT/SRID geometry insert |
| `org.flywaydb:flyway-core` | Schema versioned migrations (V001–V022+) |
| `io.lettuce:lettuce-core` | Redis client for WebSocket pub/sub delta broadcast |
| `org.slf4j:slf4j-api` | Structured logging |
| `kotlinx-serialization-json` | GeoJSON parsing in `geoJsonLineStringToWkt()` |

### External Services / APIs

| Service | URL / Protocol | Purpose |
|---|---|---|
| OpenStreetMap Tile CDN | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` | Raster map tiles (256×256 PNG) |
| [geojson.io](https://geojson.io) | Browser tool | User draws routes and exports GeoJSON |
| Redis (pub/sub) | Internal | Real-time delta broadcast to all connected WebSocket clients |
| PostgreSQL + PostGIS | JDBC | Spatial persistence: routes, geofences, vehicle locations |

---

## Frontend Implementation

### Coordinate System — The Most Important Concept

> **All projection math uses logical CSS pixels (dp), never physical pixels.**

When Compose runs on Kotlin/WASM, the browser reports both **physical pixels** and **logical CSS pixels** (dp). All OSM tile math and canvas drawing must use the same coordinate space. If they differ, tiles and vector overlays misalign across zoom changes.

- **Correct**: `BoxWithConstraints.maxWidth.value` → logical dp available synchronously on frame 1
- **Incorrect**: `onSizeChanged { size: IntSize }` → physical pixels; unavailable on first frame (causes blank flash)

This rule is enforced across both `OsmTileLayer` and `FleetMapCanvas` by sharing one `BoxWithConstraints` scope.

---

### `MapProjection.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/`

The single source of truth for all Web Mercator coordinate math. Has no Compose dependency — shared between frontend layers and unit-testable independently.

```kotlin
object MapProjection {
    const val TILE_PX = 256.0   // OSM tile size in logical pixels
    fun worldSize(zoom: Int): Double = TILE_PX * (1 shl zoom)
    fun lonToWorldX(lon: Double, zoom: Int): Double
    fun latToWorldY(lat: Double, zoom: Int): Double   // Web Mercator, clamped ±85.05°
    fun toCanvasXY(lat, lon, viewState, canvasW, canvasH): Pair<Float, Float>
    fun tileX(lon, zoom): Int
    fun tileY(lat, zoom): Int
    fun tileCanvasOffset(tx, ty, viewState, canvasW, canvasH): Pair<Float, Float>
    fun fitZoom(minLat, minLon, maxLat, maxLon, canvasW, canvasH, minZoom=5, maxZoom=18): Int
}
```

**`panned(dxPx, dyPx)` — Inverse Web Mercator for drag-to-pan**

Converts a pixel drag delta from the gesture engine into a new centre coordinate:

```
newWorldX = worldX(centerLon) - dxPx          // drag right → move west
newWorldY = worldY(centerLat) - dyPx          // drag down  → move north
newLon    = newWorldX / worldSize * 360 - 180
newLat    = atan(sinh(π × (1 - 2 × newWorldY / worldSize))) × 180/π
```

The result is clamped to ±85.05° latitude. Both world-pixel values are clamped to `[0, worldSize]` so panning can never escape the map boundary.

#### Key Functions Explained

**`latToWorldY`** — Web Mercator formula:

$$Y_{world} = \frac{1 - \frac{\ln(\tan(\phi) + \sec(\phi))}{\pi}}{2} \times worldSize(z)$$

Where $\phi$ is latitude in radians. The latitude is clamped to ±85.05° (Mercator's mathematical poles).

**`toCanvasXY`** — Converts geographic coordinates to canvas pixel position:

```
px = worldX(lon) - worldX(centerLon) + canvasWidth / 2
py = worldY(lat) - worldY(centerLat) + canvasHeight / 2
```

The viewport centre appears at exactly the middle of the canvas area. Any geo coordinate offset from the centre is translated proportionally to canvas pixel offset.

**`tileCanvasOffset`** — Same math for tile top-left corners:

```
left = tileX * TILE_PX - worldX(centerLon) + canvasWidth / 2
top  = tileY * TILE_PX - worldY(centerLat) + canvasHeight / 2
```

**`fitZoom`** — Iterates from `maxZoom` down to `minZoom`; returns the first zoom where the bounding box occupies less than 85% of both canvas dimensions. Used to auto-fit loaded routes into view on first load.

#### `MapViewState` Data Class

```kotlin
data class MapViewState(
    val centerLat: Double = 14.66015,   // Metro Manila default
    val centerLon: Double = 121.05683,
    val zoom: Int = 10,                 // city-level default
) {
    fun zoomedIn()  = copy(zoom = (zoom + 1).coerceAtMost(MAX_ZOOM))
    fun zoomedOut() = copy(zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM))
    fun panned(dxPx: Float, dyPx: Float): MapViewState  // inverse Mercator pan

    companion object {
        const val MIN_ZOOM = 3   // world fills the 1920×1080 lens at ~zoom 3
        const val MAX_ZOOM = 18
    }
}
```

**Immutable design**: every zoom/pan operation produces a new `MapViewState` via `copy()`, which triggers Compose recomposition of the entire map stack. The zoom buttons in `LiveTrackingScreen` are disabled when `mapState.zoom == MIN_ZOOM` and `mapState.zoom == MAX_ZOOM` respectively, preventing the user from zooming to a level where the fixed-lens canvas can no longer cover the visible area.

---

### `OsmTileLayer.kt`

**Location**: `composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/`

Renders the OSM raster tile grid — the bottom layer of the map stack. Accepts the fixed canvas dimensions from its parent (`FleetMapCanvas`) rather than measuring them independently.

```
┌─────────────────────────────────────────────────────────┐
│  Box(fillMaxSize)  ← parent provides canvasW / canvasH  │
│                                                         │
│   for each visible tile (ty, then tx — with X wrap):    │
│     wrappedTx = ((tx % numTiles) + numTiles) % numTiles │
│     AsyncImage(                                         │
│       model = "https://tile.openstreetmap.org/z/wrappedTx/ty.png"│
│       Modifier.absoluteOffset(left.dp, top.dp)          │
│               .requiredSize(256.dp)                     │
│     )                                                   │
└─────────────────────────────────────────────────────────┘
```

#### Tile Grid Calculation

1. Find the centre tile: `(cTx, cTy)` using `MapProjection.tileX/Y(centerLat, centerLon, zoom)`
2. Determine how many tiles fit in each direction: `halfW = ceil(canvasW / TILE_PX / 2) + 1`
3. Iterate tiles in range `(cTx - halfW)..(cTx + halfW)` × `(cTy - halfH)..(cTy + halfH)`
4. Skip Y indices < 0 or ≥ 2^zoom (poles — no tiles exist there)
5. **Wrap X indices**: `wrappedTx = ((tx % numTiles) + numTiles) % numTiles` — this makes the map seamless horizontally at all zoom levels; positioning still uses the unwrapped `tx` so tiles land in the correct screen location
6. Call `MapProjection.tileCanvasOffset()` → `(left, top)` in logical dp
7. Render each tile with `absoluteOffset(left.dp, top.dp)` + `requiredSize(256.dp)`

#### Tile Image Loading with Coil 3

```kotlin
AsyncImage(
    model    = "https://tile.openstreetmap.org/$zoom/$wrappedTx/$ty.png",
    contentDescription = null,
    modifier = Modifier
        .absoluteOffset(leftDp.dp, topDp.dp)
        .requiredSize(256.dp),
)
```

Coil 3 handles HTTP caching automatically. The `coil-network-ktor3` backend reuses the same Ktor `HttpClient` already configured in the app's DI module.

> **OSM tile usage is free and requires no API key.** The tile URL `https://tile.openstreetmap.org/{z}/{x}/{y}.png` is a public CDN operated by the OpenStreetMap Foundation. There are no request limits for reasonable non-commercial use; the only legal requirement is that the "© OpenStreetMap contributors" attribution text remains visible on the map at all times.

---

### `FleetMapCanvas.kt`

**Location**: `composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/`

The top-level map composable. Uses the **fixed-lens viewport** pattern to eliminate zoom jump artefacts while filling the parent container.

```
Box(modifier.clipToBounds()               ← LENS: fills real container, clips overflow
    .pointerHoverIcon(Hand)
    .detectDragGestures { _, delta -> onPan(delta.x, delta.y) }
) {
    Box(Modifier.requiredSize(1920.dp, 1080.dp).align(Center))  ← FIXED CANVAS
    {
        val canvasW = 1920f   // constant — never changes on zoom recomposition
        val canvasH = 1080f

        // Layer 1 — OSM Tiles (background)
        OsmTileLayer(mapState, canvasW, canvasH)     // fills inner box

        // Layer 2 — Canvas overlay (routes + vehicles)
        Canvas(Modifier.matchParentSize()) {
            // Route polylines
            routes.forEach { route ->
                val points = SvgUtils.wktToPoints(route.lineString)
                // project each point → Path → drawPath(Stroke)
            }
            // Vehicle markers
            animatedVehicles.forEach { v ->
                drawVehicleMarker(v, isSelected)
            }
        }

        // Layer 3 — Attribution (required by OSM licence)
        Box(Modifier.matchParentSize()) {
            Text("© OpenStreetMap contributors", Modifier.align(BottomEnd))
        }
    }
}
```

#### Why the Fixed-Lens Pattern?

Every call to `mapState.zoom` (or `mapState.centerLat/Lon`) triggers a full recomposition of `FleetMapCanvas`. If `canvasW`/`canvasH` were read from `BoxWithConstraints` or `onSizeChanged` inside that composable, they would also re-evaluate on every zoom step — even though the container size hasn't actually changed. The resulting microsecond difference in reported dimensions caused tiles and the vector Canvas to compute slightly different pixel offsets, making the map visibly "jump" on each zoom.

The fix is to **decouple the projection size from the layout size**:
- The outer `Box` with `clipToBounds()` is the *lens* — it takes whatever real space the layout provides and clips anything outside it.
- The inner `Box` is forced to exactly `1920×1080` dp via `requiredSize` and centred — it is the *fixed canvas*.
- `canvasW = 1920f` and `canvasH = 1080f` are plain `val`s, never re-read from Compose measurement.
- Zoom recompositions update only `mapState` — projection math is stable, tiles and vector overlay stay aligned.

#### Route Polyline Rendering

```kotlin
val pts = SvgUtils.wktToPoints(route.lineString ?: return@forEach)
val path = Path()
pts.forEachIndexed { i, (lng, lat) ->
    val (cx, cy) = MapProjection.toCanvasXY(lat, lng, mapState, canvasW, canvasH)
    if (i == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
}
drawPath(path, routeColor, style = Stroke(width = 4f))
```

`SvgUtils.wktToPoints()` parses `LINESTRING(lon lat, lon lat, …)` — note WKT uses `(longitude latitude)` order, not `(latitude longitude)`.

#### Vehicle Marker Animation

```kotlin
private data class AnimatedVehicle(
    val vehicleId: String, val lat: Float, val lon: Float, val rotation: Float
)

// Smooth animation using Compose animation APIs
val animLat  by animateFloatAsState(v.latitude,  tween(500))
val animLon  by animateFloatAsState(v.longitude, tween(500))
val animRot  by animateFloatAsState(v.headingDeg, tween(500))
```

Each vehicle position change triggers a 500ms ease animation so markers glide smoothly rather than jumping between positions.

#### `drawVehicleMarker()` — Canvas Extension

Draws an equilateral triangle (pointing north) rotated by `headingDeg`:

```
1. save()
2. translate(canvasX, canvasY)
3. rotate(headingDeg)
4. drawPath(triangle, vehicleColor)     // filled triangle
5. if (selected) drawCircle(glow ring)  // selection highlight
6. restore()
```

The triangle size is larger for the selected vehicle. The glow ring uses a semi-transparent circle behind the fill.

---

### `FleetTrackingViewModel.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/features/tracking/`

Central state holder for the tracking screen. Manages every observable piece of UI state.

#### Constructor Dependencies

```kotlin
class FleetTrackingViewModel(
    private val getActiveRoutesUseCase: GetActiveRoutesUseCase,
    private val getFleetStatusUseCase:  GetFleetStatusUseCase,
    private val fleetLiveClient:        FleetLiveClient,
    private val trackingRepository:     TrackingRepository,
) : ViewModel()
```

Injected via Koin in `ViewModelModule.kt`:

```kotlin
factory {
    FleetTrackingViewModel(
        getActiveRoutesUseCase = get(),
        getFleetStatusUseCase  = get(),
        fleetLiveClient        = get(),
        trackingRepository     = get(),
    )
}
```

#### State Flows

| StateFlow | Type | Description |
|---|---|---|
| `routesState` | `UiState<List<RouteDto>>` | Loading / Success / Error for active routes |
| `fleetStatus` | `FleetStatusDto?` | Vehicle count summary from REST |
| `fleetState` | `Map<String, VehicleRouteState>` | Live position keyed by vehicleId |
| `selectedVehicleId` | `String?` | Currently selected vehicle |
| `mapState` | `MapViewState` | Viewport: centre lat/lon + zoom |
| `importResult` | `ImportResult?` | Success or Error after route import |
| `connectionState` | `ConnectionState` | WebSocket state enum |
| `isRefreshing` | `Boolean` | Refresh in-progress flag |

#### Key Functions

**`loadMap(forceRefresh)`** — called on `init` and on refresh:

```kotlin
fun loadMap(forceRefresh: Boolean = false) = viewModelScope.launch {
    _routesState.value = UiState.Loading
    val routes = getActiveRoutesUseCase(forceRefresh)
    _routesState.value = when (routes) {
        is Result.Success -> {
            autoCenterOnRoutes(routes.data)   // auto-fit viewport
            UiState.Success(routes.data)
        }
        is Result.Error -> UiState.Error(routes.message)
    }
    // also loads fleet status concurrently
}
```

**`autoCenterOnRoutes(routes)`** — computes bounding box of all route linestrings:

```kotlin
fun autoCenterOnRoutes(routes: List<RouteDto>) {
    val bbox = SvgUtils.boundingBox(routes.mapNotNull { it.lineString })
        ?: return
    val bestZoom = MapProjection.fitZoom(
        minLat = bbox.minLat, minLon = bbox.minLng,
        maxLat = bbox.maxLat, maxLon = bbox.maxLng,
        canvasW = 1024f, canvasH = 640f,   // reasonable reference canvas size
    )
    _mapState.value = MapViewState(
        centerLat = (bbox.minLat + bbox.maxLat) / 2,
        centerLon = (bbox.minLng + bbox.maxLng) / 2,
        zoom      = bestZoom,
    )
}
```

Note: The reference canvas `1024×640` is a conservative estimate — the actual canvas may be larger. `fitZoom` uses 85% fill so there is always margin; the result is approximate but always shows all routes.

**`importRoute(name, description, geojson)`** — saves a new route via REST:

```kotlin
fun importRoute(name: String, description: String?, geojson: String) {
    viewModelScope.launch {
        val result = trackingRepository.createRoute(name, description, geojson)
        _importResult.value = when (result) {
            is Result.Success -> {
                // Append new route to existing success state
                val current = (_routesState.value as? UiState.Success)?.data ?: emptyList()
                _routesState.value = UiState.Success(current + result.data)
                ImportResult.Success
            }
            is Result.Error -> ImportResult.Error(result.message)
        }
    }
}
```

**`collectDeltas()`** — ingests WebSocket delta stream:

```kotlin
init {
    viewModelScope.launch {
        fleetLiveClient.deltas.collect { delta ->
            val current = _fleetState.value[delta.vehicleId]
            _fleetState.value = _fleetState.value.toMutableMap().apply {
                put(delta.vehicleId,
                    if (current != null) DeltaDecoder.merge(current, delta)
                    else DeltaDecoder.fromDelta(delta))
            }
        }
    }
    viewModelScope.launch {
        fleetLiveClient.connectionState.collect { _connectionState.value = it }
    }
    fleetLiveClient.connect()
    loadMap()
}
```

**`zoomIn() / zoomOut() / pan(dx, dy)`** — viewport interaction:

```kotlin
fun zoomIn()  { _mapState.value = _mapState.value.zoomedIn() }
fun zoomOut() { _mapState.value = _mapState.value.zoomedOut() }
fun pan(dx: Float, dy: Float) { _mapState.value = _mapState.value.panned(dx, dy) }
```

`pan()` is called on every drag frame from `FleetMapCanvas.detectDragGestures`. Because `panned()` is pure math on a data class, the state update is instantaneous.

#### `ImportResult` Sealed Interface

```kotlin
sealed interface ImportResult {
    data object Success : ImportResult
    data class  Error(val message: String) : ImportResult
}
```

---

### `FleetLiveClient.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/tracking/`

Manages the persistent WebSocket connection to the backend for real-time fleet deltas.

#### Architecture

```kotlin
class FleetLiveClient(
    private val wsBaseUrl:     String,           // wss://api.yourdomain.com
    private val tokenProvider: TokenProvider,    // provides JWT token
) {
    val deltas: SharedFlow<VehicleStateDelta>    // emits every incoming delta
    val connectionState: SharedFlow<ConnectionState>
    
    fun connect()      // starts connectWithRetry() in background coroutine
    fun disconnect()   // cancels job, emits Disconnected
    fun close()        // cancels scope, closes HttpClient
}
```

#### Connection State Machine

```
IDLE → connect() → Connecting
Connecting → handshake OK → Connected
Connected → frame → emits VehicleStateDelta
Connected → closed → Reconnecting(attempt=1)
Reconnecting → retry after delay*attempt → Connected | Error
```

Reconnection uses exponential back-off: `delay(RECONNECT_DELAY_MS * attempts)` (3 000ms × attempt number), up to `MAX_RECONNECT_ATTEMPTS = 5`.

#### Authentication

The JWT token is appended as a query parameter: `wss://…/v1/fleet/live?token=<jwt>`. The backend's Ktor WebSocket route validates this token before upgrading the connection.

#### Delta Processing

```kotlin
for (frame in incoming) {
    if (frame is Frame.Text) {
        runCatching {
            json.decodeFromString<VehicleStateDelta>(frame.readText())
        }.onSuccess { _deltas.emit(it) }
    }
}
```

`runCatching` silently drops malformed frames — the stream is resilient to corrupted JSON without crashing the flow.

#### `ConnectionState` Sealed Interface

```kotlin
sealed interface ConnectionState {
    data object Connecting                        : ConnectionState
    data object Connected                         : ConnectionState
    data class  Reconnecting(val attempt: Int)   : ConnectionState
    data object Disconnected                      : ConnectionState
    data class  Error(val message: String)        : ConnectionState
}
```

---

### `DeltaDecoder.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/`

Applies partial WebSocket updates (deltas) onto full vehicle state objects. Only fields that are non-null in the delta overwrite the existing state.

```kotlin
object DeltaDecoder {
    fun merge(current: VehicleRouteState, delta: VehicleStateDelta): VehicleRouteState =
        current.copy(
            latitude      = delta.latitude      ?: current.latitude,
            longitude     = delta.longitude     ?: current.longitude,
            speedKph      = delta.speedKph      ?: current.speedKph,
            headingDeg    = delta.headingDeg    ?: current.headingDeg,
            routeId       = delta.routeId       ?: current.routeId,
            routeProgress = delta.routeProgress ?: current.routeProgress,
            recordedAt    = delta.recordedAt    ?: current.recordedAt,
        )

    fun fromDelta(delta: VehicleStateDelta): VehicleRouteState  // bootstrap absent vehicle
}
```

**Why deltas instead of full state?** Reduces WebSocket payload size. A typical GPS ping (lat, lon, speed, heading) is ~80 bytes as a delta vs ~300 bytes for the full `VehicleRouteState`. At 60 updates/min per vehicle across a 200-vehicle fleet, this saves ~2.6 MB/min of bandwidth.

---

### `SvgUtils.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/`

Utility functions for converting PostGIS WKT geometry into renderable data.

#### `wktToPoints(lineString)`

```kotlin
fun wktToPoints(lineString: String): List<Pair<Double, Double>>
// Input:  "LINESTRING(121.1037 14.7021, 121.1036 14.7024)"
// Output: [(121.1037, 14.7021), (121.1036, 14.7024)]   ← (lng, lat)
```

**Important**: WKT stores coordinates as `(longitude latitude)`, not `(latitude longitude)`. The caller must pass them to `MapProjection.toCanvasXY(lat=y, lon=x)` in the correct order.

#### `getPointAtProgress(lineString, progress)`

Interpolates a position at `progress ∈ [0.0, 1.0]` along a LINESTRING using linear interpolation over cumulative Euclidean segment lengths. Used to animate vehicles along their route when a WebSocket delta triggers smooth motion.

#### `boundingBox(lineStrings)`

Returns the geographic bounding box of multiple WKT linestrings — used by `FleetTrackingViewModel.autoCenterOnRoutes()` to compute the area to fit into the viewport.

#### `BoundingBox.project()` (Legacy)

```kotlin
fun project(lng, lat, canvasW, canvasH): Pair<Float, Float>
```

A simple bounding-box normalisation with 5% padding — the original schematic projection before OSM tiles were added. Now superseded by `MapProjection.toCanvasXY()` for the map view, but retained for potential future use (mini-map, export, etc.).

---

### `TrackingRepository.kt` / `TrackingRepositoryImpl.kt`

**Location**: `composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/repository/`

Data access layer wrapping `FleetApiClient` with short-lived caches.

```kotlin
interface TrackingRepository {
    suspend fun getFleetStatus(): Result<FleetStatusDto>
    suspend fun getVehicleState(vehicleId: String): Result<VehicleStateDto>
    suspend fun getLocationHistory(vehicleId: String): Result<List<LocationHistoryEntry>>
    suspend fun getActiveRoutes(forceRefresh: Boolean): Result<List<RouteDto>>
    suspend fun createRoute(name: String, description: String?, geojson: String): Result<RouteDto>
}
```

#### Caching Strategy

```kotlin
class TrackingRepositoryImpl(private val api: FleetApiClient) : TrackingRepository {
    private val statusCache = SimpleCache<FleetStatusDto>(ttlMs = 30_000)
    private val routesCache = SimpleCache<List<RouteDto>>(ttlMs = 30_000)

    override suspend fun getActiveRoutes(forceRefresh: Boolean): Result<List<RouteDto>> {
        if (!forceRefresh) routesCache.get("routes")?.let { return Result.Success(it) }
        return api.getActiveRoutes().also { if (it is Result.Success) routesCache.put("routes", it.data) }
    }

    override suspend fun createRoute(...): Result<RouteDto> =
        api.createRoute(CreateRouteRequest(name, description, geojson))
            .also { if (it is Result.Success) routesCache.invalidate("routes") }
    // ↑ cache invalidation ensures next getActiveRoutes() fetches fresh data including new route
}
```

The 30-second TTL means fleet status and routes don't hammer the server on every recomposition. `forceRefresh=true` (bound to the Refresh button) always bypasses the cache.

---

### `ImportRouteDialog.kt`

**Location**: `composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/`

Material 3 `AlertDialog` for the route import workflow.

```
┌────────────────────────────────────────────────┐
│  Import Route from GeoJSON                     │
│                                                │
│  Draw a route at geojson.io, then copy the     │
│  GeoJSON output and paste it below.            │
│                                                │
│  Route Name *          [________________]      │
│  Description (opt.)    [________________]      │
│  GeoJSON *             [                ]      │
│                        [  multi-line    ]      │
│                        [________________]      │
│                                                │
│  ⚠ Error message (if any)                      │
│                                                │
│  [Cancel]              [⟳] [Import]           │
└────────────────────────────────────────────────┘
```

#### State & Validation

```kotlin
var name        by remember { mutableStateOf("") }
var description by remember { mutableStateOf("") }
var geojson     by remember { mutableStateOf("") }

val canImport = name.isNotBlank() && geojson.isNotBlank() && !isLoading
```

The `Import` button is disabled until both required fields have non-blank content and no import is in flight.

#### Import Workflow

1. User clicks **Import** → `onImport(name.trim(), description.trimOrNull(), geojson.trim())` fires
2. Parent (`LiveTrackingScreen`) sets `importLoading = true`, calls `vm.importRoute(…)`
3. `FleetTrackingViewModel` sends `POST /v1/tracking/routes` via `TrackingRepository`
4. `LaunchedEffect(importResult)` in `LiveTrackingScreen` reacts:
   - `ImportResult.Success` → closes dialog, clears loading state
   - `ImportResult.Error` → sets `importError` message, keeps dialog open

---

### `LiveTrackingScreen.kt`

**Location**: `composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/`

The root screen composable. Wires all sub-components together.

#### Layout Structure

```
Column (fillMaxSize)
  ├── Row (weight=1)
  │     ├── Box (weight=1)  ← Map area (drag-to-pan via FleetMapCanvas)
  │     │     ├── UiState switch:
  │     │     │     Loading → CircularProgressIndicator (centred)
  │     │     │     Error   → error text (centred)
  │     │     │     Success → FleetMapCanvas(routes, fleetState, selectedId, mapState,
  │     │     │                                onPan = { dx, dy -> vm.pan(dx, dy) })
  │     │     └── Column (Alignment.TopEnd)  ← Map controls
  │     │           ├── [↺] Refresh button  (vm.refresh())
  │     │           ├── [+] Zoom in button  (vm.zoomIn(), disabled at MAX_ZOOM=18)
  │     │           ├── [−] Zoom out button (vm.zoomOut(), disabled at MIN_ZOOM=3)
  │     │           └── [↑] Import button   (showImportDialog = true)
  │     ├── Box (1dp divider)
  │     └── Column (width=240dp)  ← Vehicle sidebar
  │           ├── Header: "Vehicles  N active"
  │           ├── LazyColumn: VehicleSidebarRow per vehicle
  │           └── VehicleInfoPanel (if vehicle selected)
  └── ConnectionStatusBar (bottom)
```

#### `VehicleSidebarRow`

Each row shows:
- A coloured dot: green (live WebSocket), amber (tracked, no WS), gray (offline)
- Vehicle display name (license plate or vehicleId)
- Speed text from live `fleetState` or fallback from `fleetStatus`
- Route progress percentage (if available)

The sidebar data preferentially uses `fleetStatus.vehicles` (REST, contains full vehicle registry) augmented by live `fleetState` values, falling back to `fleetState` alone if the REST status is not yet loaded.

#### `VehicleInfoPanel`

Displayed below the sidebar list when a vehicle is selected. Shows:

| Field | Source |
|---|---|
| ID | `VehicleRouteState.vehicleId` |
| Speed | `speedKph` formatted with 1 decimal place |
| Heading | `headingDeg` rounded to integer degrees |
| Progress | `routeProgress` shown as 1-decimal percentage |
| Route ID | `routeId` |

---

## Backend Implementation

### `TrackingRoutes.kt`

**Location**: `fleet-management/src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/http/`

All HTTP and WebSocket endpoints for the tracking module.

#### Endpoint Summary

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/v1/tracking/routes/active` | Public | Fetch all routes with WKT polylines |
| `POST` | `/v1/tracking/routes` | JWT | Create a new route from GeoJSON |
| `POST` | `/v1/tracking/vehicles/{id}/location` | JWT | Ingest a GPS ping |
| `GET` | `/v1/tracking/vehicles/{id}/state` | JWT | Latest position snapshot |
| `GET` | `/v1/tracking/fleet/status` | JWT | All vehicles + latest tracking state |
| `GET` | `/v1/tracking/vehicles/{id}/history` | JWT | Paginated location history |
| `WS` | `/v1/fleet/live?token=<jwt>` | JWT (query param) | Real-time delta stream |

#### `geoJsonLineStringToWkt()` — GeoJSON Parsing

```kotlin
private fun geoJsonLineStringToWkt(geojson: String): String? = runCatching {
    val root = Json.parseToJsonElement(geojson).jsonObject
    val geometry: JsonObject? = when (root["type"]?.jsonPrimitive?.content) {
        "FeatureCollection" -> root["features"]?.jsonArray?.firstOrNull()
                                    ?.jsonObject?.get("geometry")?.jsonObject
        "Feature"           -> root["geometry"]?.jsonObject
        "LineString"        -> root
        else                -> null
    }
    if (geometry?.get("type")?.jsonPrimitive?.content != "LineString") return@runCatching null
    val coords = geometry["coordinates"]?.jsonArray ?: return@runCatching null
    val wktCoords = coords.joinToString(", ") { pt ->
        "${pt.jsonArray[0].jsonPrimitive.double} ${pt.jsonArray[1].jsonPrimitive.double}"
    }
    "LINESTRING($wktCoords)"
}.getOrNull()
```

Handles all three GeoJSON export formats from geojson.io:
- `{"type":"LineString","coordinates":[…]}`
- `{"type":"Feature","geometry":{"type":"LineString","coordinates":[…]}}`
- `{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{…}}]}`

Returns `null` on parse failure → endpoint responds `422 Unprocessable Entity` with `INVALID_GEOJSON` error code.

#### `POST /v1/tracking/routes`

```kotlin
authenticate("auth-jwt") {
    route("/v1/tracking/routes") {
        post {
            val body = call.receive<CreateRouteRequest>()
            val wkt = geoJsonLineStringToWkt(body.geojson)
                ?: return@post call.respond(HttpStatusCode.UnprocessableEntity, ...)
            val route = spatialAdapter.createRoute(body.name, body.description, wkt)
            call.respond(HttpStatusCode.Created, ApiResponse.success(route, call.requestId))
        }
    }
}
```

#### `POST /v1/tracking/vehicles/{id}/location` — Resilience Chain

```
Request
  ↓
LocationUpdateRateLimiter.isAllowed(vehicleId)   → 429 if exceeded
  ↓
IdempotencyKeyManager.getCachedResponse(key)     → 200 (cached) if duplicate
  ↓
CircuitBreaker.execute {
    UpdateVehicleLocationUseCase.execute(sensorPing)
}                                                → 503 if circuit OPEN
  ↓
RedisDeltaBroadcaster.broadcast(delta)           → all WS clients receive update
  ↓
200 OK + idempotency cache write
```

The circuit breaker (`CircuitBreaker("LocationUpdate", failureThreshold=5)`) opens after 5 consecutive PostGIS failures, immediately returning `503 Service Unavailable` rather than exhausting the DB connection pool.

---

### `PostGISAdapter.kt`

**Location**: `fleet-management/src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/persistence/`

Encapsulates all PostGIS spatial SQL operations using the Exposed ORM.

#### `findAllRoutes()`

```kotlin
fun findAllRoutes(): List<RouteDTO> = transaction {
    val wktExpr = SpatialFunctions.asText(RoutesTable.polyline)   // ST_AsText()
    RoutesTable.select(RoutesTable.id, RoutesTable.name, RoutesTable.description, wktExpr)
        .map { RouteDTO(id = it[RoutesTable.id].value.toString(), ..., lineString = it[wktExpr]) }
}
```

`ST_AsText(polyline)` converts the PostGIS binary geometry to WKT format (`LINESTRING(lon lat, …)`) that the frontend can parse with `SvgUtils.wktToPoints()`.

#### `createRoute(name, description, wktLineString)`

```kotlin
fun createRoute(name: String, description: String?, wktLineString: String): RouteDTO = transaction {
    val geom = PGgeometry("SRID=4326;$wktLineString")
    val id = RoutesTable.insertAndGetId {
        it[RoutesTable.name]        = name
        it[RoutesTable.description] = description
        it[RoutesTable.polyline]    = geom
    }
    RouteDTO(id = id.value.toString(), name = name, description = description, lineString = wktLineString)
}
```

`SRID=4326` specifies WGS 84 (standard GPS coordinates). PostGIS stores the geometry in an efficient binary format with a spatial GIST index.

#### `snapToRoute(location, routeId)`

Uses two PostGIS functions:
- `ST_LineLocatePoint(route, point)` → progress value `[0.0, 1.0]` of the nearest point on the route
- `ST_LineInterpolatePoint(route, progress)` → the actual snapped coordinate at that progress

This is used by `UpdateVehicleLocationUseCase` to compute each vehicle's route progress and distance-from-route when a GPS ping arrives.

---

## Database Schema

### V017 — PostGIS Extension and Spatial Tables

```sql
CREATE EXTENSION IF NOT EXISTS postgis;

-- Spatial columns on vehicles
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS last_location GEOMETRY(Point, 4326);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS route_progress DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS bearing        DOUBLE PRECISION DEFAULT 0.0;

-- Routes: stores vehicle paths as PostGIS LineStrings
CREATE TABLE IF NOT EXISTS routes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    polyline    GEOMETRY(LineString, 4326),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Geofences: depot zones, restricted areas, client sites
CREATE TABLE IF NOT EXISTS geofences (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name     VARCHAR(255) NOT NULL,
    type     VARCHAR(50) NOT NULL,          -- 'DEPOT', 'RESTRICTED', 'CLIENT_SITE'
    boundary GEOMETRY(Polygon, 4326) NOT NULL,
    ...
);

-- GIST spatial indexes for high-performance proximity queries
CREATE INDEX IF NOT EXISTS idx_vehicles_location  ON vehicles  USING GIST(last_location);
CREATE INDEX IF NOT EXISTS idx_routes_polyline    ON routes    USING GIST(polyline);
CREATE INDEX IF NOT EXISTS idx_geofences_boundary ON geofences USING GIST(boundary);
```

GIST (Generalized Search Tree) indexes enable O(log n) spatial queries for operations like `ST_LineLocatePoint` and `ST_Contains`.

### V018 — Seed Test Routes (Metro Manila)

```sql
INSERT INTO routes (name, description, polyline) VALUES
('Village Main Loop', 'Core route through the main village streets',
 ST_GeomFromText('LINESTRING(121.1037114 14.7021343, 121.1036057 14.7024576,
                             121.1035961 14.7031675, 121.1043276 14.7048466)', 4326));

INSERT INTO geofences (name, type, boundary) VALUES
('Village Entry Depot', 'DEPOT',
 ST_GeomFromText('POLYGON((121.103 14.702, 121.104 14.702, 121.104 14.703,
                           121.103 14.703, 121.103 14.702))', 4326));
```

### V019 — Location History Table

```sql
CREATE TABLE IF NOT EXISTS location_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id          VARCHAR(36)  NOT NULL,
    route_id            VARCHAR(36),
    progress            DOUBLE PRECISION NOT NULL,   -- 0.0–1.0 along route
    speed               DOUBLE PRECISION NOT NULL,
    heading             DOUBLE PRECISION NOT NULL,
    status              VARCHAR(20)  NOT NULL,        -- IN_TRANSIT | IDLE | OFF_ROUTE
    distance_from_route DOUBLE PRECISION NOT NULL,   -- metres from nearest route point
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    timestamp           TIMESTAMPTZ NOT NULL,
);

-- Composite index for per-vehicle time-series queries
CREATE INDEX idx_location_history_vehicle_timestamp ON location_history(vehicle_id, timestamp DESC);
-- Partial index: only active sessions for dashboard queries
CREATE INDEX idx_location_history_in_transit ON location_history(vehicle_id, timestamp DESC)
    WHERE status IN ('IN_TRANSIT', 'OFF_ROUTE');
```

---

## Data Transfer Objects (DTOs)

### Frontend DTOs (`TrackingDtos.kt` — commonMain)

```kotlin
// A saved named route (REST response)
@Serializable
data class RouteDto(
    val id: String? = null,
    val name: String? = null,
    val vehicleId: String? = null,
    val startedAt: Long? = null,     // epoch ms (FlexibleEpochMsSerializer)
    val endedAt: Long? = null,
    val lineString: String? = null,  // WKT: "LINESTRING(lon lat, …)"
)

// Full live vehicle state (REST snapshot)
@Serializable
data class VehicleRouteState(
    val vehicleId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    val routeId: String? = null,
    val routeProgress: Double? = null,  // 0.0–1.0
    val recordedAt: Long? = null,
)

// Partial update from WebSocket (delta only — null fields = unchanged)
@Serializable
data class VehicleStateDelta(
    val vehicleId: String,     // always present — identifies the vehicle
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKph: Double? = null,
    val headingDeg: Double? = null,
    val routeId: String? = null,
    val routeProgress: Double? = null,
    val recordedAt: Long? = null,
)

// Fleet summary (from REST /fleet/status)
@Serializable
data class FleetStatusDto(
    val totalVehicles: Int? = null,
    val activeVehicles: Int? = null,
    val vehicles: List<VehicleStatusSummary> = emptyList(),
)
```

### Request DTO (`CreateRouteRequest.kt` — commonMain)

```kotlin
@Serializable
data class CreateRouteRequest(
    val name: String,
    val description: String? = null,
    val geojson: String,   // Raw GeoJSON string; parsed server-side
)
```

---

## End-to-End Feature Walkthrough

### 1. Screen Initialisation

```
LiveTrackingScreen created
  → koinViewModel<FleetTrackingViewModel>() instantiated
  → FleetTrackingViewModel.init {
      fleetLiveClient.connect()   ← WebSocket opens, ConnectionState.Connecting emitted
      loadMap()                   ← GET /v1/tracking/routes/active
    }
  → routesState = UiState.Loading → CircularProgressIndicator shown
  → routes arrive → routesState = UiState.Success([…])
  → autoCenterOnRoutes() → mapState updated → FleetMapCanvas rendered
  → OsmTileLayer loads tiles for metro Manila at zoom≈12
  → Canvas overlay draws route polylines over tiles
```

### 2. Real-Time Vehicle Update

```
GPS chip on vehicle → POST /v1/tracking/vehicles/{id}/location
  → Rate limiter checks 60/min quota
  → Idempotency check (optional header)
  → UpdateVehicleLocationUseCase.execute(SensorPing)
      → PostGISAdapter.snapToRoute(location, routeId)
          → ST_LineLocatePoint → progress=0.43
          → ST_LineInterpolatePoint → snappedLat, snappedLon
      → INSERT INTO location_history
      → UPDATE vehicles SET last_location, route_progress, bearing
  → RedisDeltaBroadcaster.broadcast(VehicleStateDelta(vehicleId, lat, lon, speed, heading…))
      → Redis PUBLISH → all subscribed Ktor WS handlers
  → Each WebSocket handler sends JSON frame to connected browser clients

Browser client receives frame:
  → FleetLiveClient parses VehicleStateDelta JSON
  → emits to _deltas SharedFlow
  → FleetTrackingViewModel.collectDeltas():
      DeltaDecoder.merge(currentState, delta) → updated VehicleRouteState
      _fleetState[vehicleId] = updatedState
  → FleetMapCanvas recomposes
  → animateFloatAsState(lat, tween(500)) → smooth 500ms glide to new position
  → drawVehicleMarker() redraws triangle at new position with updated heading
```

### 3. Route Import Workflow

```
User → Opens geojson.io → draws LineString on map → copies GeoJSON
  ↓
Clicks [↑] Import button in LiveTrackingScreen
  ↓
ImportRouteDialog opens (name field, description, GeoJSON textarea)
  ↓
User pastes GeoJSON, enters route name → clicks [Import]
  ↓
onImport(name, description, geojson) → vm.importRoute()
  ↓
TrackingRepository.createRoute() → FleetApiClient.createRoute()
  → POST /v1/tracking/routes {name, description, geojson}
  ↓
Backend:
  geoJsonLineStringToWkt(geojson)
    → parses Feature/FeatureCollection/LineString
    → builds "LINESTRING(lon lat, …)" WKT string
  spatialAdapter.createRoute(name, description, wkt)
    → PGgeometry("SRID=4326;" + wkt)
    → INSERT INTO routes ... RETURNING id
  → 201 Created {id, name, lineString}
  ↓
Frontend ImportResult.Success:
  → New RouteDto appended to routesState
  → routesCache invalidated
  → Dialog closes
  → FleetMapCanvas immediately draws new polyline
```

---

## Key Design Decisions

### 1. Fixed-Lens Viewport (Zoom Jump Elimination)

Early implementations measured canvas dimensions dynamically (via `BoxWithConstraints` or `onSizeChanged`). Because zoom state changes trigger full recompositions of `FleetMapCanvas`, the dimension-measuring calls also re-evaluated — producing microsecond-scale differences that misaligned the tile grid and Canvas overlay on every zoom step.

The production solution decouples projection size from layout size:
- **Outer Box** (`clipToBounds`) fills whatever real space the parent provides — the *lens*.
- **Inner Box** (`requiredSize(1920.dp, 1080.dp)`) is a fixed-size canvas, centred inside the lens.
- `canvasW = 1920f` and `canvasH = 1080f` are plain constants — they survive zoom recompositions unchanged.
- The lens clips overflow so the user sees only the portion of the fixed canvas that falls within the real viewport.

Result: tiles and vector overlay always share identical projection math; zoom changes only update `mapState`.

**Rule**: Every number in the map stack — `TILE_PX`, canvas width/height, tile offsets — must be in the same unit (logical dp). The fixed `1920f`/`1080f` values are that authoritative source.

### 2. Shared Canvas Dimensions

`FleetMapCanvas` passes the fixed `canvasW = 1920f` / `canvasH = 1080f` to both `OsmTileLayer` and every `MapProjection.toCanvasXY()` call. This guarantees the tile layer and vector overlay use identical coordinate systems.

### 3. Drag-to-Pan (Inverse Web Mercator)

Drag gestures are handled by Compose's `detectDragGestures` on the outer lens `Box`. Each `dragAmount` is a small Offset in logical dp delivered on every pointer move event. `MapViewState.panned(dx, dy)` converts this pixel delta back to a new centre coordinate using the inverse Web Mercator formula:

```
newWorldX = worldX(centerLon) - dxPx    // subtracted because dragging right scrolls west
newLat    = atan(sinh(π × (1 - 2 × newWorldY / worldSize))) × (180/π)
```

The pan function clamps world pixel coordinates to `[0, worldSize]` and latitude to `±85.05°`, so the map can never be dragged off the edge of the world. The `PointerIcon.Hand` cursor shows on hover via `pointerHoverIcon`, giving familiar map UX feedback.

### 4. Horizontal Tile Wrapping

At zoom levels 3–5 the 1920×1080 canvas can be wide enough to show tiles past the international date line. Without wrapping, those positions have no valid OSM tile index (> 2^zoom − 1) and would render as gaps. The tile loop uses `wrappedTx = ((tx % numTiles) + numTiles) % numTiles` for the URL while using the raw `tx` for positioning — so tiles beyond the date line load seamlessly and land in the correct canvas position.

### 5. Zoom Level Limits

`MIN_ZOOM = 3` was chosen because at zoom 2 the entire world maps to roughly 4 × 256 = 1024 logical pixels wide — smaller than the 1920 fixed canvas, so gaps appear at the sides. At zoom 3 the world is 8 × 256 = 2048 px wide, comfortably covering 1920. The zoom-out button is disabled at `MIN_ZOOM`; zoom-in is disabled at `MAX_ZOOM = 18` (finest OSM detail level).

### 6. Immutable MapViewState

`MapViewState` is a data class — every zoom or pan produces a new instance via `copy()`. This works correctly with Compose's state diffing: any change to `mapState` triggers a recomposition of `FleetMapCanvas`, `OsmTileLayer`, and the Canvas overlay simultaneously with no intermediate invalid state.

### 7. No PostGIS in Frontend

The frontend receives geometries as WKT text (`LINESTRING(…)`) — not binary PostGIS format. `PostGISAdapter.findAllRoutes()` calls `ST_AsText(polyline)` to do the conversion server-side. Frontend only needs simple string parsing (`SvgUtils.wktToPoints()`), keeping the Kotlin/WASM bundle free of JVM-specific JDBC geometry types.

### 8. Delta vs Full State WebSocket

The WebSocket stream sends `VehicleStateDelta` (partial updates) rather than full `VehicleRouteState` snapshots to reduce payload size. `DeltaDecoder.merge()` applies only the changed fields, preserving all other fields from the last known state. This is especially important at high fleet density where 100+ vehicles may update simultaneously.

### 9. Route Import via geojson.io

Rather than building a custom polygon editor, the workflow leverages [geojson.io](https://geojson.io) — a free, powerful browser tool. Users draw routes there, copy the GeoJSON output, and paste it into `ImportRouteDialog`. The backend's `geoJsonLineStringToWkt()` handles all three variants geojson.io may export.

### 10. Auto-Center on Load

`autoCenterOnRoutes()` uses `SvgUtils.boundingBox()` + `MapProjection.fitZoom()` to automatically set the viewport to show all loaded routes on first render. The reference canvas size of `1024×640` gives a conservative estimate; `fitZoom` uses 85% fill so all routes are visible with a comfortable margin regardless of actual viewport size.

### 11. Resilience (Circuit Breaker + Rate Limiter + Idempotency)

The GPS ingestion endpoint (`POST /location`) is designed to handle high-throughput, unreliable mobile data:
- **Rate limiter**: 60 updates/min/vehicle prevents a malfunctioning device from flooding the database
- **Idempotency key**: driver apps can safely retry on network failure without creating duplicate records
- **Circuit breaker**: opens after 5 PostGIS failures, immediately returning 503 so the app server stays responsive while the DB recovers

---

## File Reference Index

### Frontend (`Fleet Management BackOffice/`)

| File | Package | Role |
|---|---|---|
| [`MapProjection.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/MapProjection.kt) | `commonMain/util` | Web Mercator math, `MapViewState` (zoom/pan/panned) |
| [`OsmTileLayer.kt`](../composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/OsmTileLayer.kt) | `webMain/tracking/components` | OSM tile grid with horizontal wrapping |
| [`FleetMapCanvas.kt`](../composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/FleetMapCanvas.kt) | `webMain/tracking/components` | Fixed-lens viewport, drag-to-pan gesture, 3-layer map stack |
| [`FleetTrackingViewModel.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/features/tracking/FleetTrackingViewModel.kt) | `commonMain/tracking` | All state flows; zoom, pan, import, WebSocket collection |
| [`FleetLiveClient.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/tracking/FleetLiveClient.kt) | `commonMain/tracking` | WebSocket client with auto-reconnect |
| [`DeltaDecoder.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/DeltaDecoder.kt) | `commonMain/util` | Merges WebSocket deltas into full state |
| [`SvgUtils.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/util/SvgUtils.kt) | `commonMain/util` | WKT parsing, bounding box, route interpolation |
| [`TrackingRepository.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/repository/TrackingRepository.kt) | `commonMain/repository` | Data access + 30s cache |
| [`ImportRouteDialog.kt`](../composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/components/ImportRouteDialog.kt) | `webMain/tracking/components` | GeoJSON import dialog |
| [`LiveTrackingScreen.kt`](../composeApp/src/webMain/kotlin/org/solodev/fleet/mngt/features/tracking/LiveTrackingScreen.kt) | `webMain/tracking` | Root screen composable |
| [`TrackingDtos.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/api/dto/tracking/TrackingDtos.kt) | `commonMain/api/dto/tracking` | All tracking-related DTOs |
| [`CreateRouteRequest.kt`](../composeApp/src/commonMain/kotlin/org/solodev/fleet/mngt/api/dto/tracking/CreateRouteRequest.kt) | `commonMain/api/dto/tracking` | Route import request body |

### Backend (`fleet-management/`)

| File | Package | Role |
|---|---|---|
| [`TrackingRoutes.kt`](../../fleet-management/src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/http/TrackingRoutes.kt) | `tracking/infrastructure/http` | All HTTP + WebSocket endpoints |
| [`PostGISAdapter.kt`](../../fleet-management/src/main/kotlin/com/solodev/fleet/modules/tracking/infrastructure/persistence/PostGISAdapter.kt) | `tracking/infrastructure/persistence` | PostGIS spatial queries |
| [`V017__Add_PostGIS.sql`](../../fleet-management/src/main/resources/db/migration/V017__Add_PostGIS.sql) | DB migration | Extension + routes + geofences tables |
| [`V018__Seed_Village_Routes.sql`](../../fleet-management/src/main/resources/db/migration/V018__Seed_Village_Routes.sql) | DB migration | Metro Manila test data |
| [`V019__create_location_history_table.sql`](../../fleet-management/src/main/resources/db/migration/V019__create_location_history_table.sql) | DB migration | Vehicle tracking history |

---

## OSM Licensing Summary

| Question | Answer |
|---|---|
| **Cost** | Free — no payment, no account, no API key |
| **Quota** | No hard quota for reasonable non-commercial/low-volume use |
| **Licence** | OpenStreetMap data: [ODbL](https://opendatacommons.org/licenses/odbl/) — open database licence |
| **Tile rendering** | Provided free by the OSMF; tiles themselves are © OpenStreetMap contributors |
| **Attribution required?** | **Yes** — "© OpenStreetMap contributors" must be visible wherever the map is displayed |
| **Commercial use allowed?** | Yes, with attribution |

The attribution `Text` composable in `FleetMapCanvas` (bottom-right corner) satisfies the licence requirement.

---

*This document reflects the live codebase as built. All coordinate system notes, design decisions, and code snippets are derived directly from the production source files.*
