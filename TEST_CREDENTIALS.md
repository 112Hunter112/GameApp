# 🔐 Test User Credentials

Use these accounts to test the different user flows in Postman or the Frontend.

## 🏢 Vendor (Venue Owner)
**Role:** `VENUE_OWNER`
**Capabilities:** Create Venues, Add Courts, View Bookings.

| Field | Value |
| :--- | :--- |
| **Email** | `Paditya2@wisc.edu` |
| **Password** | `SecurePassword123@` |
| **Phone** | `6085550199` |

> **Useful IDs for this User:**
> * **Venue ID:** `7b74167c-dd26-4268-9abf-6c18dd963c5d` (Madison Sports Center)
> * **Sport ID:** `44d0eab7-b98a-444d-964a-61ffe2de8fab` (Tennis)
> * **Court ID:** `9ff62c53-6f6e-487f-9873-3c3eac6f4928` (Court-1)

---

## 🏃 Player (Customer)
**Role:** `PLAYER` (or `USER`)
**Capabilities:** Search Venues, Book Courts, View Own Bookings.

| Field | Value |
| :--- | :--- |
| **Email** | `parth.aditya01@gmail.com` |
| **Password** | `Password123@` |
| **Phone** | *(Not set yet)* |

---

## 🛠 Quick Postman Setup
1. **Login as Vendor:**
    * `POST http://localhost:8080/api/auth/login`
    * Body: `{ "email": "Paditya2@wisc.edu", "password": "SecurePassword123@" }`

2. **Login as Player:**
    * `POST http://localhost:8080/api/auth/login`
    * Body: `{ "email": "parth.aditya01@gmail.com", "password": "Password123@" }`
