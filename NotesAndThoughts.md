# Comprehensive Frontend Guide: Venue & Location Picker for React Native

## 1. Recommended Tools & Libraries (Frontend)

| Feature | Recommended Library | Why? |
|---------|-------------------|------|
| Map Display | react-native-maps | Industry standard. Uses Apple Maps (iOS) and Google Maps (Android) but can use OSM tiles for free/open source option. |
| Search/Autocomplete | Custom + Nominatim API | Lightweight solution using OSM's search engine without heavy dependencies. |
| Pin Drop | Built-in to react-native-maps | Use `<Marker draggable />` with `onDragEnd` event for drag-and-drop functionality. |
| HTTP Requests | Built-in fetch() or axios | For making API calls to Nominatim and your backend. |

## 2. Do Users Need to Select Country/State/City Manually?

**NO.** Do NOT make users select "Country → State → City" from dropdowns. This creates poor UX.

Instead, use **Address Autocomplete** (like Uber/DoorDash):

### How It Works with OSM (Nominatim):

**User types:** "Central Park"

**Frontend calls:**
```javascript
https://nominatim.openstreetmap.org/search?q=Central+Park&format=jsonv2&addressdetails=1&limit=5
```

**API Returns:**
```json
[
  {
    "display_name": "Central Park, New York County, New York, United States",
    "lat": "40.782",
    "lon": "-73.965",
    "address": {
      "city": "New York",
      "state": "New York",
      "country": "United States"
    },
    "place_id": "25482910"
  }
]
```

**Result:** API automatically provides City, State, and Country. Send `display_name` and coordinates to your backend.

## 3. The "Pin Drop" Workflow (Reverse Geocoding)

When users don't know the address and drop a pin:

1. **User Action:** Long-press on the map
2. **App Gets:** `lat: 43.55, lon: -89.40`
3. **App Calls (Reverse Geocode):**
   ```javascript
   https://nominatim.openstreetmap.org/reverse?lat=43.55&lon=-89.40&format=json
   ```
4. **API Returns:** "123 Random Road, Madison, WI"
5. **App Fills:** Text box automatically populates with the address

## 4. API Integration Flow

### Scenario A: User Selects from Search (Nominatim)

When user picks "Madison Tennis Center" from dropdown:

**POST /api/matches/manual**
```json
{
  "opponentIds": ["..."],
  "sport": "TENNIS",
  "venueName": "Madison Tennis Center",
  "venueAddress": "123 Sport Ln, Madison, WI",
  "venueLat": 43.0731,
  "venueLng": -89.4012,
  "externalVenueId": "25482910"
}
```

### Scenario B: User Drops a Pin

When user drops a pin on a random field:

**POST /api/matches/manual**
```json
{
  "opponentIds": ["..."],
  "sport": "SOCCER",
  "venueName": "Community Field",
  "venueAddress": "Custom Location",
  "venueLat": 43.1000,
  "venueLng": -89.5000,
  "externalVenueId": null
}
```

**Key Difference:** `externalVenueId` is `null` for custom pins, contains OSM `place_id` for searched venues.

## 5. UI Components to Build

### A. Search Bar
- **Type:** Text input with debouncing (300-500ms)
- **Function:** Calls Nominatim API when user types
- **Features:** Clear button, loading indicator, error handling

### B. Result List
- **Display:** Shows `display_name` from API response
- **Interaction:** Clickable items that select the location
- **Layout:** Dropdown below search bar or modal overlay

### C. Map View
- **Current Location:** Show user's GPS location
- **Pin Drop:** Long-press to drop marker
- **Auto-center:** Center map on selected search result
- **Markers:** Different icons for searched vs. custom locations

### D. Venue Name Input
- **Conditional:** Show only for custom pins
- **Default:** Pre-fill with reverse-geocoded address
- **Validation:** Ensure non-empty for custom pins

## 6. How to Calculate Distance

### Option A: Database Way (PostGIS) - Recommended

Most accurate and efficient using PostGIS:

```sql
SELECT name, 
       ST_DistanceSphere(location, ST_MakePoint(-89.4012, 43.0731)) as distance_meters
FROM venues
WHERE ST_DWithin(location, ST_MakePoint(-89.4012, 43.0731)::geography, 10000);
```

**Parameters:**
- `location`: Your database column (Type Geometry)
- `ST_MakePoint(lng, lat)`: User's current GPS coordinates

### Option B: Frontend Way (Haversine Formula)

For UI display without database queries:

```javascript
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in km
  const dLat = deg2rad(lat2 - lat1);
  const dLon = deg2rad(lon2 - lon1);
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c; // Distance in km
}

function deg2rad(deg) {
  return deg * (Math.PI/180);
}
```
