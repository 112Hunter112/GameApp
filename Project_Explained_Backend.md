# 🏗️ Project Architecture Breakdown
**Project:** SportsBackend

This document explains the responsibility of each class and interface in the application, organized by their architectural layer.

---

## 1. The Controller Layer (The "Waiter")
*Responsible for receiving HTTP requests, validating input, and sending JSON responses.*

### `AuthController.java`
**Location:** `src/main/java/.../controller/AuthController.java`

* **Role:** The main entry point for user authentication.
* **Key Responsibilities:**
    * **`/register`:** Accepts the `RegisterRequest` JSON, validates it using `@Valid`, and passes it to `AuthService`. Returns the success message string.
    * **`/login`:** Accepts `LoginRequest`, authenticates the user, and returns an `AuthResponse` containing the JWT token.
    * **`/verify`:** Handles the email verification link (e.g., `?token=xyz`), activating the user account.
    * **Error Handling:** It wraps service calls in `try-catch` blocks. If an exception occurs (like "Email already in use"), it catches the `RuntimeException` and returns a clean `400 Bad Request` instead of crashing with a 500 error.

---

## 2. The Service Layer (The "Chef")
*Responsible for the business logic. It combines data from the database, validation rules, and utilities to perform the actual work.*

### `AuthService.java`
**Location:** `src/main/java/.../service/AuthService.java`

* **Role:** The "Brain" of the security system.
* **Key Responsibilities:**
    * **Logic:** Checks if a user already exists using `existsByEmail` and `existsByPhoneNumber`.
    * **Security:** Uses `PasswordEncoder` to hash passwords before saving them (never stores plain text).
    * **Token Generation:** Generates a random UUID for the `verificationToken` sent via email.
    * **`@Transactional`:** Wraps the register method. If the email fails to send, this annotation ensures the user is rolled back (deleted) from the database so no "ghost" accounts remain.

### `EmailService.java`
**Location:** `src/main/java/.../service/EmailService.java`

* **Role:** The specialist for external communication.
* **Key Responsibilities:**
    * **MIME Handling:** Uses `MimeMessageHelper` to create HTML-formatted emails (allowing bold text and clickable links) instead of just plain text.
    * **Sending:** Uses `JavaMailSender` to connect to Google's SMTP server.
    * **Exception Conversion:** Catches checked `MessagingException` and re-throws it as an unchecked `RuntimeException`. This is crucial because it triggers the `@Transactional` rollback in `AuthService`.

### `JwtUtil.java`
**Location:** `src/main/java/.../service/JwtUtil.java`

* **Role:** The "ID Card Printer".
* **Key Responsibilities:**
    * **`generateToken()`:** Creates a cryptographically signed string (JWT) containing the user's email and issue date.
    * **`validateToken()`:** Checks if a token coming from the frontend is valid and not expired.
    * **`extractEmail()`:** Decodes the token to find out *who* is making the request.

---

## 3. The Repository Layer (The "Pantry")
*Responsible for talking directly to the database.*

### `UserRepository.java` (Interface)
**Location:** `src/main/java/.../repository/UserRepository.java`

* **Role:** The bridge to PostgreSQL.
* **Key Responsibilities:**
    * **Magic Methods:** Extends `JpaRepository<User, UUID>`, which gives free access to methods like `.save()`, `.findAll()`, and `.delete()`.
    * **Custom Queries:** Defines specific lookups like `findByEmail()`, `existsByPhoneNumber()`, and `findByVerificationToken()`. Spring Data JPA automatically writes the SQL queries for these based on the method names.

---

## 4. The Model Layer (The "Ingredients")
*Defines the structure of our data objects.*

### `User.java`
**Location:** `src/main/java/.../model/User.java`

* **Role:** Represents a single row in the `users` database table.
* **Key Fields:**
    * `@Id UUID id`: A unique, random identifier for the user.
    * `isVerified`: A boolean flag that locks the account until the email link is clicked.
    * `verificationToken`: Stores the temporary code used for email verification.
    * `role`: Stores the user's permission level (`USER`, `ADMIN`, etc.).

### `UserRole.java` (Enum)
**Location:** `src/main/java/.../model/UserRole.java`

* **Role:** Defines the specific types of users allowed in the system.
* **Why:** Using an Enum prevents typos. You can't accidentally assign a role of "superadmin" or "Usr"; it *must* be `USER`, `ADMIN`, or `VENUE_OWNER`.

---

## 5. DTOs (Data Transfer Objects)
*Simple boxes used to carry data between the frontend and backend.*

* **`RegisterRequest.java`:**
    * Carries raw form data (name, email, password) from the App to the Controller.
    * Contains validation annotations like `@NotBlank` and `@Pattern` (regex for passwords and phone numbers) to ensure data quality.
* **`LoginRequest.java`:**
    * A simple object carrying just the email and password for login attempts.
* **`AuthResponse.java`:**
    * The "Success Packet" sent back to the user after login. It contains the generated JWT `token`, the user's `firstName`, and their `role`.

---

## 6. Configuration

### `SecurityConfig.java`
**Location:** `src/main/java/.../security/SecurityConfig.java`

* **Role:** The "Security Guard".
* **Key Responsibilities:**
    * **`securityFilterChain`:** Configures the rules. It uses `requestMatchers("/api/auth/**").permitAll()` to let unauthenticated users access the login/register pages, while locking down everything else with `.anyRequest().authenticated()`.
    * **`passwordEncoder`:** Provides the `BCryptPasswordEncoder` bean used by the `AuthService` to hash passwords.
    * **CSRF:** Disables CSRF protection because the app is stateless (JWT) and doesn't use browser sessions.

### `application.yml`
**Location:** `src/main/resources/application.yml`

* **Role:** The main configuration file.
* **Key Responsibilities:**
    * Sets up the connection to the PostgreSQL database (`datasource.url`).
    * Configures JPA/Hibernate settings (like `ddl-auto: update` to automatically create tables).
    * References secret variables (like `${EMAIL_PASSWORD}`) which are injected from the secrets file.

### `application-secrets.properties`
**Location:** `src/main/resources/application-secrets.properties`

* **Role:** The vault for sensitive data.
* **Key Responsibilities:**
    * Stores the real Gmail App Password.
    * Stores the JWT Secret Key used for signing tokens.
    * **Note:** This file is usually ignored by Git (`.gitignore`) to prevent leaking passwords.
