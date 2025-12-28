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
