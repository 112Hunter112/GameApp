# 📚 Java & Spring Boot Master Guide
**Created for:** Parth Aditya
**Project:** SportsBackend

---

## 1. Database & Transactions

### Q: What is `@Transactional`?
**The Concept:**
Think of `@Transactional` as an "All or Nothing" safety switch. It wraps a specific method in a safety bubble. If *anything* goes wrong (an exception is thrown) inside that bubble, Spring hits the **Undo Button** (Rollback) on the database, reverting all changes made during that method.

**Why we used it in `AuthService.register`:**
We perform two distinct actions in this method:
1.  Save the `User` to the database.
2.  Send an email via Google's SMTP server.

If step 1 succeeds but step 2 fails (e.g., bad wifi, wrong password), we don't want a "ghost user" in the database who exists but can never verify their account. `@Transactional` ensures that if the email fails, the user is deleted from the DB automatically.

### Q: Why did the database crash with "Column contains null values"?
**The Concept:**
In SQL, a `NOT NULL` column means every single row **must** have a value.
When you added `private Boolean isVerified = false;` to your Java model, you were telling the database: "Create a new column called `is_verified` and make sure it is never empty."

**The Problem:**
You had old users (from 4 days ago) in the database. When the database tried to add the new column to those old rows, it didn't know what value to put there. It defaulted to `NULL`, which violated your `NOT NULL` rule.

**The Fix:**
We used manual SQL (`ALTER TABLE users ADD COLUMN... DEFAULT FALSE`) to tell the database: "For all the old guys, just set this value to False."

---

## 2. Authentication & Security

### Q: What is JWT (JSON Web Token)?
**The Concept:**
A JWT is a "Digital ID Card" that is cryptographically signed.
* **Old Way (Sessions):** The server keeps a list of logged-in users in memory. If the server restarts, everyone gets logged out.
* **New Way (JWT):** The server gives the user a signed token (the ID card). The user holds onto it.

**Why we use it:**
It makes the backend **Stateless**. The server doesn't need to remember who is logged in. When the mobile app sends a request with the token attached, the server checks the signature and knows, "Ah, this is Parth, and he is a verified User."

### Q: What is `SecurityConfig` and the "Bouncer"?
**The Concept:**
Spring Security is like a vault door—it locks everything by default. `SecurityConfig` is where you give instructions to the security guard (The Bouncer).

**Our Configuration:**
* `.requestMatchers("/api/auth/**").permitAll()`: "Let anyone into the lobby (Register/Login pages)."
* `.anyRequest().authenticated()`: "Check ID (JWT) for every other room."
* `.csrf(disable)`: We disable CSRF (Cross-Site Request Forgery) because that protection is for browser sessions, and we are using mobile JWTs.

### Q: What is `BCryptPasswordEncoder`?
**The Concept:**
You should **never** store passwords in plain text (e.g., "Password@123"). If a hacker stole your database, they would have everyone's passwords.
`BCrypt` scrambles the password into a random string (hash) like `$2a$10$WsK86n...`. It is a "one-way" function—you can turn a password into a hash, but you cannot turn a hash back into a password.

---

## 3. Email & Messaging

### Q: What is `MimeMessage` vs `SimpleMailMessage`?
**The Difference:**
* **`SimpleMailMessage`:** Like a sticky note. Pure text. No formatting.
* **`MimeMessage`:** Like a webpage. Supports **HTML**, bold text, colors, and clickable links.

**Why we switched:**
You needed the user to click a link (`<a href="...">Verify</a>`). You cannot create a clickable hyperlink in a `SimpleMailMessage`.

### Q: What is `MimeMessageHelper`?
**The Concept:**
Java's native email code is very old and complex. `MimeMessageHelper` is a utility class provided by Spring that acts as a "Wrapper." It hides the ugly low-level code and gives you easy methods like `.setTo()`, `.setSubject()`, and `.setText(html, true)`.

### Q: Why did I get "Authentication Failed" when sending email?
**The Concept:**
You cannot use your standard Gmail password for code access anymore. Google considers code (like your Spring Boot app) as a "Less Secure App."
You had to generate an **App Password** (a 16-character code) that is specifically generated for this application to log in securely without needing 2-Factor Authentication every time.

---

## 4. API & Controller Logic

### Q: What is `ResponseEntity<?>`?
**The Concept:**
In Java, a method usually has to return one specific thing (e.g., an `Integer`).
But in a REST API, sometimes you want to return an Object (Success) and sometimes a String (Error Message).

`ResponseEntity<?>` uses a "Wildcard" (`?`). It tells Java: "I am going to return an HTTP Response, but the body of that response might be different types depending on what happens."

### Q: What does `@Valid` do?
**The Concept:**
It is the automated Gatekeeper.
In your `RegisterRequest` DTO, you added annotations like `@NotBlank` and `@Email`.
When you put `@Valid` in the Controller (`public void register(@Valid ...)`), Spring checks all those rules **before** the code even runs. If the email is invalid, Spring blocks the request immediately.

### Q: Why use `try-catch` in the Controller?
**The Concept:**
If your Service layer throws an error (like "Email already exists") and nobody catches it, the server panics and returns a `500 Internal Server Error`. This looks bad to the user.
By using `try-catch`, we catch that panic and convert it into a calm `400 Bad Request` with a nice message explaining exactly what went wrong.

---

## 5. Maven & Project Configuration

### Q: What is `pom.xml`?
**The Concept:**
POM stands for **Project Object Model**. It is the "Recipe" for your application.
It tells Maven (the chef):
1.  **Dependencies:** What ingredients (libraries) to download from the internet (e.g., Spring Web, Postgres Driver, JWT).
2.  **Plugins:** What tools to use to build the code.
3.  **Versions:** Which version of Java and Spring Boot to use.

### Q: Why was I getting "Cannot resolve symbol 'jakarta'"?
**The Concept:**
Your `pom.xml` was asking for **Spring Boot 4.0.0**, which does not exist yet.
Because the version was wrong, Maven couldn't find the "Parent" project.
Because it couldn't find the Parent, it refused to download *any* dependencies.
Without dependencies, your IDE didn't know what `jakarta` or `springframework` was, so it marked everything in red.

### Q: What does "Reload Maven Project" do?
**The Concept:**
Changing the text in `pom.xml` doesn't instantly change the project structure. You have to "Reload" to force the IDE to read the file again, go to the internet, download the new libraries, and index them.

---

## 6. Git & Version Control

### Q: What is the "LF will be replaced by CRLF" warning?
**The Concept:**
* **LF (Line Feed):** How Linux/Mac computers mark the end of a line (Typewriter style: push paper up).
* **CRLF (Carriage Return + Line Feed):** How Windows computers mark the end of a line (Typewriter style: slide carriage back + push paper up).

**The Warning:**
Git is smart. It stores files in the Linux format (LF) in the cloud so everyone is compatible.
When you are on Windows, Git says: "I'm converting these files to Windows format (CRLF) so they look right in your text editor, but I'll convert them back when you push." **It is a safe warning to ignore.**
