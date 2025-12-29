# 📚 Java & Spring Boot Master FAQ
**Project:** Sports Application Backend  
**Topic:** Backend Development, JPA Modeling & Hibernate

---

## 🛠 Core Spring Boot Concepts

### 1️⃣ What is @Transactional?
A safety wrapper ensuring **"All or Nothing"** database behavior. If any operation inside the method fails, Spring rolls back all DB changes to prevent data corruption.
* **Example:** Used in `AuthService.register` because saving a user and sending a verification email must both succeed together.

### 2️⃣ What is JWT (JSON Web Token)?
A **stateless** authentication token that functions like a digitally signed ID card.
* It allows the server to verify a user's identity without storing session data in server memory.

### 3️⃣ What is SecurityConfig?
A configuration class that defines the security rules for your application:
* **Public Endpoints:** Paths like `/api/auth/**` are permitted for everyone to allow registration and login.
* **Protected Routes:** Any other request requires a valid JWT to be authenticated.
* **CSRF:** Disabled for this REST API to simplify authentication for mobile clients (React Native).

### 4️⃣ What does @Valid do?
Triggers automatic validation of a DTO (Data Transfer Object) before the controller logic executes.
* It checks annotations like `@NotBlank` or `@Email` and stops invalid requests immediately with a 400 Bad Request error.

---

## 🧱 JPA & Database Modeling Deep Dive

### 5️⃣ @ManyToOne vs @OneToMany: Which side "owns" the link?
In a standard relationship (like Venue and Courts):
* **@ManyToOne (The "Child" Side):** This is the **Owner**. It creates the actual foreign key column in the database (e.g., `venue_id`).
* **@OneToMany (The "Parent" Side):** This is the **Inverse** side. It uses the `mappedBy` attribute to reference the field in the child class and does not create a new column.

### 6️⃣ What is @JoinColumn?
This annotation is used to explicitly **name** the foreign key column in your database table.
* It lives on the **Owner** of the relationship.
* **Benefit:** It ensures your Java model matches your SQL schema exactly (e.g., `@JoinColumn(name="sport_id")`).

### 7️⃣ Handling Many-to-Many with Extra Data (Bridge Entities)
When a relationship needs its own data (like a user's `proficiency_level` for a specific sport), you use a **Bridge Entity** instead of a simple `@ManyToMany`.
* **The Structure:** Create a dedicated entity (e.g., `UserPreference`) that has `@ManyToOne` links to both parent entities.

### 8️⃣ What are @EmbeddedId and @MapsId?
These are used for **Composite Primary Keys** in bridge tables.
* **@EmbeddedId:** Points to an `@Embeddable` class containing the multiple IDs that form the primary key.
* **@MapsId:** Syncs the ID from the parent entity (e.g., `User`) into the correct field of the composite ID class automatically.

### 9️⃣ Why use BigDecimal for Money?
**Double** and **Float** can lead to tiny rounding errors during calculations.
* **BigDecimal:** Provides absolute precision for currency. Always use it for `hourly_rate`, `total_price`, and financial transactions.

### 🔟 How to map PostgreSQL "jsonb" and "text[]"?
* **For jsonb (e.g., openingHours):** Use `@JdbcTypeCode(SqlTypes.JSON)` on a `Map<String, Object>` field.
* **For text[] (e.g., amenities):** Use `@ElementCollection`. This stores a simple list of strings in a separate table without requiring a full Java entity.

---

## 🏗 Architecture & Logic

### 11️⃣ What is the Service Layer?
The **"Brain"** of your application. While Entities hold data and Repositories fetch data, the Service Layer handles the **Verbs**:
* **Orchestration:** Coordinates multiple repositories to complete a task.
* **Business Rules:** Enforces logic, such as "Only verified users can book a court."

### 12️⃣ Sports.max_players vs. Courts.capacity
* **Sports.max_players:** The standard rules for the sport (e.g., 10 players for Basketball).
* **Courts.capacity:** The physical limit of that specific court (e.g., a small court might only fit 8 people safely).
* **The Logic:** A match is only valid if the number of players is within **both** limits.

### 13️⃣ What is the Vertical Slice flow?
A development strategy where you build one feature completely (from Database to Controller) before moving to the next.
* **Typical Flow:** `Entity` -> `Repository` -> `Service` -> `Controller`.

---

## 💡 Quick Tips
* **mappedBy** always points to the **Java variable name** in the owning class, not the table name.
* Always include a **No-Arg Constructor** in your Entities, as JPA requires it to instantiate objects.
* Use **camelCase** in Java; Hibernate will automatically convert it to **snake_case** in PostgreSQL.


Here is the comprehensive guide to the knowledge and concepts you gained during this troubleshooting session. This covers the why and how of the technologies you are using, rather than just the code fixes.

Markdown

# Backend Development & DevOps Knowledge Guide

## 1. Troubleshooting 403 Forbidden Errors
A **403 Forbidden** error means the server knows who you are (or that you are anonymous) but refuses to give you access to the resource. In Spring Boot, this is almost always caused by **Spring Security**.

### Common Causes & Fixes
* **The "Deny All" Default:**
    * **Concept:** Spring Security is "secure by default." If you don't explicitly allow an endpoint, it blocks it.
    * **The Fix:** You must configure a `SecurityFilterChain` bean. Use `.requestMatchers("/path/**").permitAll()` to open public endpoints like Login or Register.
* **URL Mismatches (The "Missing Slash" Trap):**
    * **Concept:** Security rules are precise. If your config allows `/api/auth/**` but your controller is mapped to `api/auth` (no leading slash), Spring treats them as different paths. The request falls through to the "catch-all" rule (usually "Authenticate Everything"), resulting in a 403.
    * **The Fix:** Always ensure your `@RequestMapping` paths in Controllers exactly match your Security Config patterns.
* **CSRF (Cross-Site Request Forgery):**
    * **Concept:** A security feature that blocks state-changing requests (POST, PUT, DELETE) from browsers unless a special token is present.
    * **The Fix:** For stateless REST APIs (like used by mobile apps or Postman), you usually disable this: `.csrf(csrf -> csrf.disable())`.

---

## 2. Docker vs. Docker Compose
You are using two distinct tools that work together to run your application.

### The `Dockerfile` (The Blueprint)
* **Purpose:** Defines how to build a **single** container image. It is like a recipe for a cake.
* **How it works in your project:**
    1.  **`FROM`**: Starts with a lightweight Linux OS with Java installed (`eclipse-temurin:17-jre-alpine`).
    2.  **`COPY`**: Moves your compiled code (`.jar` file) and your **secrets file** (`application-secrets.properties`) inside the container.
    3.  **`ENTRYPOINT`**: Tells the container what command to run when it turns on (`java -jar app.jar`).

### The `docker-compose.yml` (The Conductor)
* **Purpose:** Manages **multiple** containers at once. It defines the relationships, networking, and environment variables between them.
* **Key Concepts:**
    * **Services:** The different parts of your app. You have `db` (Postgres) and `app` (Spring Boot).
    * **Networking:** Docker Compose creates a private network. Your app can talk to the database using the hostname `db` instead of an IP address because they are in the same "compose" group.
    * **Volumes:** (`postgres_data:/var/lib/postgresql/data`) This ensures that even if you delete the database container, your actual data (users, matches) is saved on your hard drive and isn't lost.

---

## 3. Essential Commands Cheat Sheet

### Maven (Build Tool)
Used to compile your Java code and package it into a `.jar` file.
* **Run App Locally:** `./mvnw spring-boot:run` (Compiles and starts the server on your machine).
* **Build JAR File:** `./mvnw clean package` (Creates the `.jar` file in the `target/` folder, required before building a Docker image).
* **Clean Build:** `./mvnw clean install` (Wipes old files and rebuilds everything from scratch; good for fixing weird errors).

### Docker (Container Tool)
* **Start Containers:** `docker-compose up -d`
    * `up`: Create and start containers.
    * `-d`: Detached mode (runs in the background so it doesn't lock up your terminal).
* **Stop Containers:** `docker-compose down` (Stops and removes the containers).
* **Check Status:** `docker ps` (Shows running containers) or `docker ps -a` (Shows all containers, even stopped ones).
* **View Logs:** `docker logs -f <container_name>` (Stream logs from a specific container, e.g., `docker logs -f sports-db`).
* **Rebuild Images:** `docker-compose up -d --build` (Forces Docker to re-read your `Dockerfile` and compile a new image, useful if you changed Java code).

---

## 4. Development Workflow Summary
This is the "Hybrid" workflow we established for you, which is best for active development:

1.  **Database:** Run it in Docker (`docker-compose up -d db`). This keeps your machine clean and ensures you always have the correct Postgres version.
2.  **Application:** Run it locally (`./mvnw spring-boot:run`). This allows for faster restarting and easier debugging than running the app inside Docker.
3.  **Configuration:** Your `application.yml` is smart. It looks for the environment variable `DB_HOST`.
    * **Locally:** It defaults to `localhost`.
    * **In Docker:** It uses the container name `db`.
