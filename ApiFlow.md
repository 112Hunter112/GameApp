# 📘 Sports App API Documentation & Workflow Guide

This document outlines the API endpoints implemented in the Backend Core (Inventory & Auth Layers). It describes how the Frontend (React Native/Web) should interact with the Backend.

---

## 🔐 1. Authentication Module
**Base URL:** `/api/auth`

These endpoints handle user identity. The `token` returned here MUST be saved by the frontend (e.g., in SecureStore or LocalStorage) and sent in the header of all subsequent authorized requests as `Authorization: Bearer <token>`.

### Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/register` | Create a new user account. | `RegisterRequest` (email, password, firstName, lastName, role, phone) | `200 OK` (Confirmation Message) |
| **POST** | `/login` | Authenticate existing user. | `LoginRequest` (email, password) | `200 OK` + `AuthResponse` (Token, Role, Name) |
| **GET** | `/verify` | Verify email address (clicked from email link). | Query Param: `?token=...` | `200 OK` (Success Message) |

### 🔄 Auth Flow
1.  **User Sings Up:** Frontend sends `POST /register`.
2.  **User Logs In:** Frontend sends `POST /login`.
3.  **Frontend Action:** The backend responds with a JSON object containing the JWT `token`.
4.  **Storage:** The Frontend **must store this token**.
5.  **Future Requests:** For every request below that says "Requires Auth", the frontend must add this header:
    ```text
    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
    ```

---

## 🏟️ 2. Venue Management (Vendor Flow)
**Base URL:** `/api/venues`

These endpoints allow "Venue Owners" to manage their physical locations (buildings/complexes).

### Endpoints

| Method | Endpoint | Access | Description | Input |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/` | **Vendor** | Create a new Venue. | `VenueRequest` (Name, Address, Lat, Lon, Hours) |
| **GET** | `/my-venues` | **Vendor** | Get all venues owned by the logged-in user. | *None* (Uses Token) |
| **GET** | `/{venueId}` | **Public** | Get details of a single venue. | Path Var: `venueId` |
| **PUT** | `/{venueId}` | **Vendor** | Update venue details. | `VenueRequest` |
| **DELETE** | `/{venueId}` | **Vendor** | Delete a venue. | Path Var: `venueId` |
| **GET** | `/search` | **User** | Find venues near a location. | Query Params: `lat`, `lon`, `radius` |

### 🔄 Vendor Workflow: Creating Inventory
1.  **Vendor Logs In:** Receives Token.
2.  **Create Venue:** Vendor fills out form "Downtown Complex".
    * Frontend calls `POST /api/venues` with body data.
    * Backend returns created object with `id: "uuid-111"`.
3.  **View Dashboard:** Vendor clicks "My Venues".
    * Frontend calls `GET /api/venues/my-venues`.
    * Backend returns list: `[{id: "uuid-111", name: "Downtown Complex"}]`.

---

## 🎾 3. Court Management (Inventory)
**Base URL:** `/api` (Note: Some endpoints are nested under `/venues`)

Courts are the actual bookable resources (e.g., "Tennis Court 1") inside a Venue.

### Endpoints

| Method | Endpoint | Access | Description | Input |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/venues/{venueId}/courts` | **Vendor** | Add a court to a specific venue. | `CourtRequest` (Name, SportID, Price) |
| **GET** | `/venues/{venueId}/courts` | **Public** | List all courts in a venue. Supports filtering. | Path: `venueId`, Query: `?sportId=...` |
| **GET** | `/courts/{courtId}` | **Public** | Get details of one specific court. | Path: `courtId` |
| **PUT** | `/courts/{courtId}` | **Vendor** | Update court (e.g., change price). | `CourtRequest` |
| **DELETE** | `/courts/{courtId}` | **Vendor** | Remove a court. | Path: `courtId` |
| **GET** | `/my-courts` | **Vendor** | See ALL courts owned by this vendor across all venues. | *None* (Uses Token) |
| **GET** | `/courts/sport/{sportId}` | **Public** | Find all courts of a specific sport type (e.g., "All Tennis"). | Path: `sportId` |

### 🔄 Vendor Workflow: Adding Courts
*Prerequisite: Vendor has created a Venue (ID: `uuid-111`).*
1.  **Select Venue:** Vendor clicks "Manage" on "Downtown Complex".
2.  **Add Court:** Vendor fills form "Court 1", Price "$20", Sport "Tennis".
3.  **Frontend Call:** Frontend takes the Venue ID (`uuid-111`) from the previous screen.
    * Calls `POST /api/venues/uuid-111/courts`.
4.  **Backend Action:** Links the new court to Venue `uuid-111` and the Vendor.

---

## 🔍 4. User Discovery Flow (The "Search" Experience)

This is how a standard user finds a place to play.

### Step 1: Search for Venues
* **User Action:** Opens app, grants location permission.
* **Frontend Call:** `GET /api/venues/search?lat=40.71&lon=-74.00&radius=10`
* **Result:** List of venues nearby (e.g., "Downtown Complex", "Uptown Gym").

### Step 2: View Venue Details
* **User Action:** Clicks on "Downtown Complex".
* **Frontend Logic:** Frontend remembers the ID of the clicked item (`uuid-111`).
* **Frontend Call:** `GET /api/venues/uuid-111` (To show address/hours).

### Step 3: View Available Courts
* **User Action:** User scrolls down to see what they can book.
* **Frontend Call:** `GET /api/venues/uuid-111/courts`
* **Optional Filter:** If user clicked "Tennis" specifically on the home screen, Frontend calls:
    * `GET /api/venues/uuid-111/courts?sportId=uuid-tennis`
* **Result:** List of courts: "Court 1 ($20)", "Court 2 ($25)".

---

## 🛠 Frontend Developer Cheatsheet

**1. Headers are Key:**
Never forget the Authorization header for Vendor actions.
`Authorization: Bearer <token_string>`

**2. IDs come from previous calls:**
* To get a `venueId` for creating a court, you must first call `GET /my-venues`.
* To get a `sportId` for creating a court, you must first call `GET /api/sports` (Assuming you implemented the SportsController from Day 6).

**3. Hierarchical URLs:**
Notice that creating a court requires the **Venue ID** in the URL:
`/api/venues/{THIS_IS_THE_PARENT_ID}/courts`
This ensures the backend knows exactly where to put the new court.
