# Ride-Hailing / Fleet Management System Prompt

## Objective
Design a modern ride-hailing platform similar to Grab or Uber with three main components:

1. Customer Mobile App
2. Driver Mobile App
3. Admin / Backoffice Web Portal

The system should support real-time ride booking, driver dispatching, GPS tracking, payments, and fleet management.

---

# System Architecture

The platform will consist of the following layers:

Customer App (Android / iOS)
Driver App (Android / iOS)
Admin Backoffice (Web)
Backend API
Real-time Dispatch System
Database
Map / Geolocation Services

Example architecture:

Customer App → Backend API → Dispatch System → Driver App  
Driver App → Backend API → Location Updates → Customer Tracking  
Admin Portal → Backend API → Fleet Monitoring

---

# Customer Mobile App (Passenger)

## Core Features

### User Account
- User registration
- Login / authentication
- Profile management
- Phone number verification
- Payment method management

### Ride Booking
- Select pickup location
- Select destination
- View estimated fare
- Choose vehicle type
- Confirm ride request

### Map & Location
- Real-time map view
- GPS pickup detection
- Driver location tracking
- Estimated arrival time

### Ride Status
- Searching for driver
- Driver assigned
- Driver arriving
- Trip in progress
- Trip completed

### Driver Information
- Driver name
- Vehicle details
- Plate number
- Driver rating
- Contact driver

### Payments
- Cash payment
- Digital wallet
- Credit/debit card
- Fare breakdown

### Trip History
- Past rides
- Trip receipts
- Rebook previous ride

### Ratings & Feedback
- Rate driver
- Provide trip feedback
- Report issues

### Notifications
- Ride updates
- Driver arrival alerts
- Payment confirmation

---

# Driver Mobile App

## Core Features

### Driver Authentication
- Driver login
- Driver profile
- Vehicle registration
- Document verification

### Driver Availability
- Go online / offline
- Accept ride requests
- Auto or manual ride acceptance

### Ride Requests
- Receive trip request
- View passenger pickup location
- Accept or decline ride
- Navigation to pickup point

### Navigation
- Turn-by-turn navigation
- Route optimization
- Traffic-aware routing

### Trip Management
- Arrived at pickup
- Start trip
- End trip
- Fare calculation

### Earnings Dashboard
- Daily earnings
- Weekly earnings
- Trip summaries
- Incentives / bonuses

### Driver Map
- Nearby ride demand
- Heat map (optional)
- Current passenger location

### Trip History
- Completed trips
- Ride details
- Payment summaries

### Ratings
- Passenger ratings
- Driver performance stats

### Notifications
- New ride alerts
- Trip updates
- System announcements

---

# Admin / Backoffice Portal

## Fleet Management
- Monitor active drivers
- View vehicle status
- Manage driver accounts

## Trip Monitoring
- Live trip tracking
- Trip analytics
- Ride logs

## Customer Management
- User accounts
- Support tickets
- Issue resolution

## Pricing Management
- Fare configuration
- Surge pricing
- Promotions

## Reporting
- Driver earnings reports
- Platform revenue
- Usage statistics

---

# Technical Requirements

## Mobile Apps
- Kotlin Multiplatform or Native
- Jetpack Compose / Compose Multiplatform
- Real-time GPS tracking
- Offline support

## Backend
- REST API
- WebSockets for real-time updates
- Authentication service
- Dispatch matching algorithm

## Maps
- OpenStreetMap
- GeoJSON
- Routing service (OSRM or similar)

## Database
- PostgreSQL
- PostGIS for geospatial queries

---

# Real-Time Features

- Driver location streaming
- Ride request broadcasting
- Real-time trip tracking
- Dispatch matching algorithm

---

# Goal

Build a scalable ride-hailing ecosystem capable of supporting:

- thousands of drivers
- thousands of concurrent ride requests
- real-time location tracking
- fleet management and analytics