# Kotlin Multiplatform Fleet Map Backoffice Guide

## Overview

This guide explains how to build a **fleet visualization and route management system** using:

- OpenStreetMap (map data source)
- geojson.io (route drawing and exporting)
- Kotlin Multiplatform (KMP)
- Kotlin/JS + Compose for Web
- Ktor backend
- SVG rendering for map visualization

The system allows administrators to draw routes, upload them to the backend, and visualize vehicles moving along those routes in real time.

---

## Architecture Overview

```
Admin draws route → geojson.io → Export GeoJSON → Upload to Backoffice
  → Backend stores route → Frontend loads route → SVG renders route
  → Vehicles animate on route
```

---

## Step 1 — Draw Routes with geojson.io

Open <https://geojson.io> and:

1. Draw a LineString route
2. Export the data as GeoJSON

Example output:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "route_id": "route_1",
        "name": "QC Loop"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [121.108, 14.699],
          [121.109, 14.700],
          [121.110, 14.701]
        ]
      }
    }
  ]
}
```

This JSON file will be uploaded to the backoffice.

---

## Step 2 — Backend Route Model (Kotlin Multiplatform)

Shared data models:

```kotlin
@Serializable
data class Route(
    val id: UUID,
    val name: String,
    val coordinates: List<GeoPoint>
)

@Serializable
data class GeoPoint(
    val lat: Double,
    val lng: Double
)
```

---

## Step 3 — Convert GeoJSON to Route

Backend conversion example:

```kotlin
fun geoJsonToRoute(feature: GeoJsonFeature): Route {
    val coordinates = feature.geometry.coordinates.map {
        GeoPoint(
            lat = it[1],
            lng = it[0]
        )
    }
    return Route(
        id = UUID.randomUUID(),
        name = feature.properties["name"] ?: "Route",
        coordinates = coordinates
    )
}
```

---

## Step 4 — Store Routes

### Option A: PostGIS (Recommended)

Store routes using a `LINESTRING` geometry:

```sql
LINESTRING(121.108 14.699, 121.109 14.700)
```

### Option B: JSON Storage

Database table:

| Column       | Type        |
|--------------|-------------|
| `id`         | UUID        |
| `name`       | VARCHAR     |
| `geojson`    | JSONB       |
| `created_at` | TIMESTAMPTZ |

---

## Step 5 — Backend API (Ktor)

Example route endpoints:

```kotlin
routing {
    route("/routes") {
        post {
            val geojson = call.receive<GeoJsonFeatureCollection>()
            val route = geoJsonToRoute(geojson.features.first())
            routeRepository.save(route)
            call.respond(route)
        }

        get {
            call.respond(routeRepository.getAll())
        }
    }
}
```

---

## Step 6 — Frontend Rendering Strategy

Routes are rendered using SVG paths. Vehicle movement is determined by progress along the route.

Example conversion from coordinates to SVG path:

```kotlin
fun geoPointsToSvgPath(points: List<GeoPoint>): String {
    return buildString {
        append("M ${points.first().lng} ${points.first().lat}")
        points.drop(1).forEach {
            append(" L ${it.lng} ${it.lat}")
        }
    }
}
```

---

## Step 7 — Vehicle Position Along Route

Vehicle movement is determined by a `progress` value:

- `0.0` → start of route
- `1.0` → end of route

SVG method used: `getPointAtLength()` — interpolates vehicle position along the rendered path.

---

## Step 8 — Backoffice UI Features

### Route Manager

- Import GeoJSON
- List Routes
- Preview Route
- Edit Route

### Map Editor Workflow

1. Open geojson.io
2. Draw route
3. Export JSON
4. Upload in Backoffice
5. Save route

---

## Recommended Project Structure

```
shared/
  models/
    Route.kt
    GeoPoint.kt

backend/
  routes/
    RouteController.kt
    RouteService.kt

web/
  components/
    FleetMap.kt
    RouteLayer.kt
  state/
    RouteState.kt
```

---

## Kotlin Specialist Prompt Template

Use this prompt for implementing new features:

```
You are a senior Kotlin Multiplatform engineer.

System Context:
- Kotlin Multiplatform project
- Backend: Ktor
- Frontend: Kotlin/JS + Compose for Web
- Visualization: SVG map rendering
- Map data source: OpenStreetMap
- Route editor: geojson.io
- Realtime updates via WebSocket
- Vehicles move along predefined routes

Requirements:
1. Implement a route management system
2. Accept GeoJSON uploads from geojson.io
3. Convert GeoJSON LineString to internal Route model
4. Store routes in database
5. Expose routes via API
6. Render routes using SVG path in Compose Web
7. Animate vehicle position along route
8. Use Kotlin Flow / StateFlow for reactive updates

Constraints:
- Kotlin 1.9+
- Coroutine-based architecture
- No blocking calls
- Multiplatform compatible models
- Type-safe serialization with kotlinx.serialization

Output:
1. Data models
2. GeoJSON parser
3. Ktor API routes
4. Frontend state management
5. SVG rendering component
6. Vehicle animation logic
7. Unit tests
```