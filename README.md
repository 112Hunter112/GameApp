# GameApp

How to Run Both Together:
Step 1: Run Backend (Terminal 1)
bash# In your SportsBackend folder
cd C:\Users\Parth Aditya\projects\SportsBackend
# Run in IntelliJ or:
./mvnw spring-boot:run
Backend now runs on: http://localhost:8080
Step 2: Run Frontend (Terminal 2)
bash# In your GameApp-Mobile folder (when you create it)
cd C:\Users\Parth Aditya\projects\GameApp-Mobile
npm start
Mobile app runs on your phone/emulator
Step 3: Configure API Base URL in Frontend
typescript// src/services/api.ts
import axios from 'axios';

const API_BASE_URL = __DEV__
? 'http://10.0.2.2:8080/api'  // Android emulator
: 'https://your-production-api.com/api';

const api = axios.create({
baseURL: API_BASE_URL,
});

export default api;
```

---

## **Real-World Flow:**

1. User opens app → Sees "Find Venues" button
2. User taps button → Frontend calls `GET http://localhost:8080/api/venues`
3. Backend receives request → Queries PostgreSQL database
4. Backend returns JSON: `[{id: 1, name: "Soccer Field A"}, ...]`
5. Frontend receives data → Displays venues on screen

---

## **Important Notes:**

### **For Development (Local Testing):**
- Backend: `http://localhost:8080`
- Frontend connects to `localhost` (or `10.0.2.2` for Android emulator)

### **For Production (Real App):**
- Backend: Deploy to cloud (e.g., `https://api.yourgame.com`)
- Frontend: Update API URL to production URL
- Both run on different servers, communicate via HTTPS

---

## **Visual Diagram:**
```
┌─────────────────┐         HTTP Request          ┌──────────────────┐
│  React Native   │ ──────────────────────────> │  Spring Boot API │
│   (Frontend)    │    GET /api/venues           │    (Backend)     │
│   Port: 8081    │ <────────────────────────── │   Port: 8080     │
└─────────────────┘    JSON Response             └──────────────────┘
│
↓
┌──────────────┐
│  PostgreSQL  │
│   Database   │
└──────────────┘

Next Steps for You:
Phase 1: Build Backend APIs (What we're doing now)

Create /api/auth/login
Create /api/venues
Create /api/bookings
Test with Postman (you don't need frontend yet!)

Phase 2: Build Frontend (Later)

Create React Native app
Use axios or fetch to call your APIs
Display the data

