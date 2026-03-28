# Driver Mobile App Integration Guide - Vehicle Incident Reporting

This document outlines the API integration for the Android driver application to report vehicle health status and incidents.

## Overview

Drivers can report issues found during or after a trip (e.g., engine light, flat tire, brake issues). Every report will be reflected in the fleet management dashboard and maintenance page for immediate action.

## API Endpoints

### 1. Report Vehicle Incident
**Endpoint**: `POST /api/v1/vehicles/{vehiclePlate}/incidents`

**Description**: Reports a new incident/issue for the vehicle currently being used by the driver.

**Request Header**:
- `Authorization`: `Bearer <token>`

**Path Parameter**:
- `vehiclePlate`: The license plate of the vehicle (e.g., `ABC-1234`).

**Request Body** (JSON):
```json
{
  "title": "Engine Warning Light",
  "description": "The check engine light turned on during the trip. No noticeable performance drop yet.",
  "severity": "MEDIUM",
  "odometerKm": 45230,
  "location": {
      "latitude": 14.5995,
      "longitude": 120.9842
  }
}
```

**Severity Levels**:
- `LOW`: Minor issues (e.g., small scratch, interior light fuse).
- `MEDIUM`: Requires attention soon (e.g., unusual noise, scheduled service message).
- `HIGH`: Requires immediate attention (e.g., check engine light, worn brakes).
- `CRITICAL`: Vehicle unsafe to drive (e.g., brake failure, overheating, flat tire).

**Response** (201 Created):
```json
{
  "id": "inc_9876543210",
  "status": "REPORTED",
  "reportedAt": "2026-03-28T20:15:13Z"
}
```

---

### 2. Get Vehicle Status
**Endpoint**: `GET /api/v1/vehicles/{vehiclePlate}/status`

**Description**: Retrieves the current health status of a vehicle.

**Response** (200 OK):
```json
{
  "vehiclePlate": "ABC-1234",
  "healthStatus": "WARNING",
  "activeIncidentsCount": 2,
  "latestIncident": {
      "title": "Engine Warning Light",
      "severity": "MEDIUM",
      "reportedAt": "2026-03-28T20:15:13Z"
  }
}
```

**Health Statuses**:
- `HEALTHY`: No active incidents.
- `WARNING`: One or more LOW/MEDIUM incidents.
- `DANGER`: One or more HIGH incidents.
- `CRITICAL`: One or more CRITICAL incidents.

---

## Integration Notes for Android App

1. **Offline Support**: If the driver is in an area with poor connectivity, the app should queue incident reports locally and retry when online.
2. **Photos**: (Phase 2) Support for multipart/form-data will be added for photographic evidence.
3. **Immediate Alerts**: If a `CRITICAL` incident is reported, the driver should be advised to stop the vehicle and wait for assistance.
