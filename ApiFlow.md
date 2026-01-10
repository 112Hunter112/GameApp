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

# 🎾 Sports App API Documentation

## 🔐 Authentication

All endpoints require JWT authentication.

### Request Headers

```text
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

### Token Handling

- Tokens expire after 24 hours
- Refresh token endpoint: `POST /api/auth/refresh`
- Logout endpoint: `POST /api/auth/logout`
- Store token securely (use React Context or secure storage)

## 1. Match Management

### 📝 Log a Manual Match

Create a new match entry manually for any sport.

**Endpoint:** `POST /api/matches/manual`  
**Access:** User, Admin

#### Request Body Structure

```json
{
  "sport": "Tennis",
  "date": "2023-11-25T14:30:00",
  "score": "6-4, 6-3",
  "notes": "Played at Central Park",
  "winningTeam": "TEAM_A",
  "teammateIds": ["uuid-user-1"],
  "opponentIds": ["uuid-user-2"],
  "opponentEmails": ["friend@gmail.com"]
}
```

#### Field Details

| Field | Type | Required | Description | Frontend UI Notes |
|-------|------|----------|-------------|-------------------|
| sport | String | Yes | Valid sport name from predefined list | Use dropdown with: "Tennis", "Soccer", "Basketball", "Table Tennis", "Badminton" |
| date | String (ISO) | Yes | Match date and time | Use datetime picker (YYYY-MM-DDTHH:MM:SS format) |
| score | String | Yes | Free text score summary | DYNAMIC FIELD - see Sport-Specific Layouts below |
| notes | String | No | Additional comments | Textarea field (max 500 chars) |
| winningTeam | String | Yes | Must be: "TEAM_A", "TEAM_B", or "DRAW" | Radio buttons: "My Team", "Opponent Team", "Draw" |
| teammateIds | Array[UUID] | No | Teammates (your team) | Multi-select user picker (from friends list) |
| opponentIds | Array[UUID] | No | Opponents who are app users | Multi-select user picker (from friends/contacts) |
| opponentEmails | Array[String] | No | Emails for non-app opponents | Input with tag functionality (max 5 emails) |

### 🎯 Sport-Specific Input Layouts

**Important:** The score input field should change format based on the selected sport. Here are the requirements:

#### 🎾 Tennis

**Score Format Examples:**
- Singles: `6-4, 6-3`
- Doubles: `6-4, 4-6, 7-5`
- Tiebreak: `7-6(5), 6-4`

**UI Components:**
- Sets Input: Provide 3-5 set fields
- Tiebreak Checkbox: Adds tiebreak input
- Match Type: Singles/Doubles toggle (affects teammate selection)

#### ⚽ Soccer

**Score Format Examples:**
- `3-1`
- `2-2 (draw)`
- `4-0`

**UI Components:**
- Simple Score Input: Two number inputs (Home / Away)
- Overtime Toggle: Adds extra time inputs
- Penalty Shootout Section (if applicable)

#### 🏀 Basketball

**Score Format Examples:**
- `98-95`
- `112-108`

**UI Components:**
- Team Scores: Two number inputs
- Quarter Scores Toggle: Expandable section for quarter-by-quarter
- Overtime Counter (adds extra periods)

#### 🏓 Table Tennis

**Score Format Examples:**
- `11-8, 11-9, 5-11, 11-6`
- `3-2 (best of 5 games)`

**UI Components:**
- Games Input: Up to 7 games (best of 7)
- Points per Game: Each game shows two number inputs
- Deuce Option for games beyond 11 points

#### 🏸 Badminton

**Score Format Examples:**
- `21-15, 19-21, 21-17`
- `2-1 (sets)`

**UI Components:**
- Sets Input: 2-3 sets (singles/doubles)
- Points per Set: Two number inputs per set

### Frontend Implementation Guide

```javascript
// Example state structure for dynamic form
const [matchForm, setMatchForm] = useState({
  sport: 'Tennis',
  date: '',
  score: '',
  notes: '',
  winningTeam: 'TEAM_A',
  teammateIds: [],
  opponentIds: [],
  opponentEmails: []
});

// Dynamically render score input based on sport
const renderScoreInput = () => {
  switch (matchForm.sport) {
    case 'Tennis':
      return <TennisScoreInput value={matchForm.score} onChange={handleScoreChange} />;
    case 'Soccer':
      return <SoccerScoreInput value={matchForm.score} onChange={handleScoreChange} />;
    // ... other sports
    default:
      return <TextInput value={matchForm.score} onChange={handleScoreChange} />;
  }
};

// Update teammates/opponents based on sport type
const updateParticipantLimits = (sport) => {
  const limits = {
    'Tennis': { minTeammates: 0, maxTeammates: 1, minOpponents: 1, maxOpponents: 1 },
    'Soccer': { minTeammates: 0, maxTeammates: 10, minOpponents: 1, maxOpponents: 11 },
    'Basketball': { minTeammates: 0, maxTeammates: 4, minOpponents: 1, maxOpponents: 5 },
    'Table Tennis': { minTeammates: 0, maxTeammates: 1, minOpponents: 1, maxOpponents: 1 },
    'Badminton': { minTeammates: 0, maxTeammates: 1, minOpponents: 1, minOpponents: 1 }
  };
  return limits[sport] || { minTeammates: 0, maxTeammates: 10, minOpponents: 1, maxOpponents: 10 };
};
```

### 📜 Get Match History

Retrieve paginated list of past matches.

**Endpoint:** `GET /api/matches`

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | number | 0 | Page number (0-indexed) |
| size | number | 10 | Items per page (10-50 allowed) |
| sport | string | (all) | Filter by sport type |
| status | string | (all) | Filter by verification status |

#### Response Example

```json
{
  "content": [
    {
      "id": "match-uuid",
      "date": "2023-11-20T10:00:00",
      "sport": "Tennis",
      "score": "6-4",
      "winningTeam": "TEAM_A",
      "verificationStatus": "CONFIRMED",
      "source": "MANUAL_ENTRY",
      "participants": [
        {
          "userId": "user-uuid-1",
          "name": "John Doe",
          "teamName": "TEAM_A",
          "status": "ACCEPTED",
          "isHost": true
        },
        {
          "userId": "user-uuid-2",
          "name": "Jane Smith",
          "teamName": "TEAM_B",
          "status": "ACCEPTED",
          "isHost": false
        }
      ]
    }
  ],
  "totalPages": 5,
  "totalElements": 48,
  "number": 0,
  "size": 10
}
```

## 2. Dashboard Endpoints

Optimized for Home Screen widgets.

### 📅 Upcoming Matches

**Endpoint:** `GET /api/matches/upcoming`  
**Response:** Array of MatchResponse objects with future dates

### ☀️ Today's Matches

**Endpoint:** `GET /api/matches/today`  
**Response:** Array of MatchResponse objects for current day

### ⚠️ Pending Verifications (Inbox)

Matches where you need to confirm/deny the result.

**Endpoint:** `GET /api/matches/pending-verifications`

**Response:** Array of matches where:
- User is on Team B (opponent)
- verificationStatus is PENDING
- User's participation status is ACCEPTED

### ❓ Matches Without Results

Past matches with no score logged.

**Endpoint:** `GET /api/matches/incomplete`

**Response:** Array of matches where:
- Date is in the past
- score field is null or empty

## 3. User Statistics

### 📊 Overall Stats

**Endpoint:** `GET /api/matches/stats/overall`

#### Response Example

```json
{
  "totalMatches": 50,
  "wins": 30,
  "losses": 20,
  "draws": 0,
  "winRate": 60.0,
  "favoriteSport": "Tennis",
  "streak": 3,
  "lastMatchDate": "2023-11-25T14:30:00"
}
```

### 🗓️ Monthly Stats

**Endpoint:** `GET /api/matches/stats/monthly?month=11&year=2023`

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| month | number | Current month | 1-12 (January = 1) |
| year | number | Current year | Full year (2023) |

#### Response Example

```json
{
  "month": "NOVEMBER",
  "year": 2023,
  "totalMatches": 10,
  "wins": 8,
  "losses": 2,
  "draws": 0,
  "winRate": 80.0,
  "sportBreakdown": [
    { "sport": "Tennis", "matches": 6, "wins": 5 },
    { "sport": "Soccer", "matches": 4, "wins": 3 }
  ]
}
```

### 🔥 Current Streak

**Endpoint:** `GET /api/matches/stats/streak`

#### Response Example

```json
{
  "type": "WIN",
  "count": 5,
  "startDate": "2023-11-20",
  "lastMatch": {
    "id": "match-uuid",
    "date": "2023-11-25T14:30:00",
    "sport": "Tennis",
    "score": "6-4, 6-3"
  }
}
```

**Possible type values:**
- `WIN`: Winning streak
- `LOSS`: Losing streak
- `NONE`: No active streak

## 4. Social & Rivals

### ⚔️ Head-to-Head

Compare stats against a specific opponent.

**Endpoint:** `GET /api/matches/head-to-head/{opponentId}`

#### Response Example

```json
{
  "opponent": {
    "id": "opponent-uuid",
    "name": "John Doe",
    "avatarUrl": "https://example.com/avatar.jpg"
  },
  "totalMatches": 12,
  "myWins": 7,
  "opponentWins": 5,
  "draws": 0,
  "winRate": 58.3,
  "lastMatch": {
    "id": "match-uuid",
    "date": "2023-11-25T14:30:00",
    "sport": "Tennis",
    "score": "6-4, 6-3",
    "winningTeam": "TEAM_A"
  },
  "sportBreakdown": [
    { "sport": "Tennis", "total": 8, "myWins": 5 },
    { "sport": "Soccer", "total": 4, "myWins": 2 }
  ]
}
```

### 🏆 Most Played Opponents

**Endpoint:** `GET /api/matches/opponents/most-played?limit=5`

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | number | 5 | Number of opponents to return (1-20) |

#### Response Example

```json
[
  {
    "opponent": {
      "id": "user-uuid-1",
      "name": "John Doe",
      "avatarUrl": "https://example.com/avatar1.jpg"
    },
    "totalMatches": 25,
    "myWins": 15,
    "opponentWins": 10,
    "lastPlayed": "2023-11-25T14:30:00"
  },
  {
    "opponent": {
      "id": "user-uuid-2",
      "name": "Jane Smith",
      "avatarUrl": "https://example.com/avatar2.jpg"
    },
    "totalMatches": 18,
    "myWins": 12,
    "opponentWins": 6,
    "lastPlayed": "2023-11-20T10:00:00"
  }
]
```

## 5. Actions (Verify & Update)

### ✅ Verify Match

Approve or reject a match submitted by an opponent.

**Endpoint:** `POST /api/matches/{matchId}/verify`

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| approve | boolean | Yes | true to confirm, false to reject |

#### Example Requests

```javascript
// Approve a match
fetch(`/api/matches/${matchId}/verify?approve=true`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});

// Reject a match
fetch(`/api/matches/${matchId}/verify?approve=false`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
```

**Important Rules:**
- Only opponents (Team B) can verify matches
- Teammates (Team A) cannot verify their own submissions
- Once verified, status becomes CONFIRMED or REJECTED
- All participants receive notifications when verified

### ✏️ Update Score

Edit an existing match's score or result.

**Endpoint:** `PUT /api/matches/{matchId}/score`

#### Request Body

```json
{
  "score": "7-5, 6-4",
  "winningTeam": "TEAM_B",
  "notes": "Updated after review"
}
```

**Important Notes:**
- Only the match creator (host) can update scores
- Updating resets verification to PENDING
- All participants receive notifications
- Previous verification status is logged for audit

## 6. Notifications System

### 🔔 Get My Notifications

**Endpoint:** `GET /api/notifications`

#### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| unreadOnly | boolean | false | Filter to unread notifications |
| type | string | (all) | Filter by notification type |
| limit | number | 50 | Maximum notifications to return |

#### Response Example

```json
[
  {
    "id": "notif-uuid-1",
    "type": "MATCH_INVITE",
    "title": "Match Result Pending",
    "message": "Parth logged a match result against you.",
    "isRead": false,
    "referenceId": "match-uuid",
    "actionUrl": "/matches/match-uuid",
    "metadata": {
      "sport": "Tennis",
      "opponentName": "Parth",
      "score": "6-4, 6-3"
    },
    "createdAt": "2023-11-25T10:00:00",
    "expiresAt": "2023-12-02T10:00:00"
  },
  {
    "id": "notif-uuid-2",
    "type": "MATCH_VERIFIED",
    "title": "Match Verified",
    "message": "John confirmed your match result.",
    "isRead": true,
    "referenceId": "match-uuid",
    "actionUrl": "/matches/match-uuid",
    "metadata": {
      "verificationStatus": "CONFIRMED"
    },
    "createdAt": "2023-11-24T15:30:00"
  }
]
```

### 🔴 Get Unread Count

For badge/notification count.

**Endpoint:** `GET /api/notifications/unread-count`

#### Response Example

```json
{
  "count": 3,
  "breakdown": {
    "matchInvites": 2,
    "systemAlerts": 1,
    "friendRequests": 0
  }
}
```

### 📖 Mark as Read

**Endpoint:** `PUT /api/notifications/{id}/read`

**Mark all as read:** `PUT /api/notifications/read-all`

## 7. Error Handling

### Common HTTP Status Codes

| Code | Meaning | Typical Response |
|------|---------|------------------|
| 200 | Success | Requested data |
| 201 | Created | New resource with ID |
| 400 | Bad Request | Validation errors |
| 401 | Unauthorized | Invalid/missing token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Server Error | Internal server issue |

### Error Response Format

```json
{
  "timestamp": "2023-11-25T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "score",
      "message": "Score format is invalid for Tennis"
    },
    {
      "field": "winningTeam",
      "message": "Winning team must be specified"
    }
  ],
  "path": "/api/matches/manual"
}
```

## 8. Real-Time Updates (WebSocket)

For live notification updates.

### Connection

```javascript
const socket = new WebSocket(`wss://api.example.com/ws?token=${jwtToken}`);
```

### Events

```json
{
  "type": "NEW_NOTIFICATION",
  "data": {
    "notification": { ... }
  }
}
```

```json
{
  "type": "MATCH_UPDATED",
  "data": {
    "matchId": "match-uuid",
    "status": "CONFIRMED"
  }
}
```

## 9. Rate Limiting

- **Authentication endpoints:** 5 requests per minute
- **Match creation:** 10 requests per hour
- **General endpoints:** 100 requests per minute
- **Notifications:** 50 requests per minute

**Headers included in responses:**

```text
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1648226400
```

## Appendix: Key Enums & Constants

### Winning Team

```typescript
enum WinningTeam {
  TEAM_A = 'TEAM_A',    // Creator's team
  TEAM_B = 'TEAM_B',    // Opponent's team
  DRAW = 'DRAW'         // Match was a draw
}
```

### Verification Status

```typescript
enum VerificationStatus {
  PENDING = 'PENDING',      // Awaiting opponent confirmation
  CONFIRMED = 'CONFIRMED',  // All opponents confirmed
  REJECTED = 'REJECTED',    // Opponent disputed
  CANCELLED = 'CANCELLED'   // Match cancelled
}
```

### Participation Status

```typescript
enum ParticipationStatus {
  PENDING = 'PENDING',      // Invitation sent
  ACCEPTED = 'ACCEPTED',    // Joined the match
  REJECTED = 'REJECTED',    // Declined invitation
  LEFT = 'LEFT'            // Left after accepting
}
```

### Sport Types

```typescript
const SPORTS = [
  'Tennis',
  'Soccer',
  'Basketball',
  'Table Tennis',
  'Badminton',
  'Volleyball',
  'Baseball',
  'Hockey'
];
```

## Frontend Implementation Checklist

### ✅ Required Components

- Match Form with dynamic sport-specific layout
- Score Input Components for each sport type
- Participant Selector with friend search
- Verification Modal for pending matches
- Notification Badge with real-time updates
- Stats Dashboard with charts/graphs
- Match History with filters and pagination

### ✅ State Management

- Store JWT token securely
- Cache frequently accessed data (friends list, sports)
- Implement optimistic updates for match creation
- Handle offline scenarios gracefully

### ✅ UI/UX Considerations

- Show loading states for API calls
- Display error messages clearly
- Confirm destructive actions (delete, reject)
- Provide undo functionality where possible
- Implement pull-to-refresh for lists

### ✅ Testing Scenarios

- Create a Tennis match with a teammate
- Create a Soccer match with multiple opponents
- Verify a match as an opponent
- Update a match score
- Handle network errors gracefully
