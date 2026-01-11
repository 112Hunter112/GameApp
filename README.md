# SportsApp Backend Service

## Overview

The SportsApp Backend is a robust, scalable RESTful API designed to facilitate sports venue booking and management. Built on the Spring Boot framework and backed by PostgreSQL, this service handles user authentication, venue data management, and booking transactions. The architecture is currently evolving to include high-performance microservices using Go and Redis to ensure scalability under heavy load.

## Technology Stack

### Core Frameworks

- **Language:** Java 17+ (Core API), Go (High-Performance Edge Services)
- **Framework:** Spring Boot 3.x
- **Build Tool:** Maven

### Data & Security

- **Database:** PostgreSQL
- **Caching & State:** Redis (Planned for Rate Limiting & Session Caching)
- **Security:** Spring Security, BCrypt, JWT (JSON Web Tokens)
- **ORM:** Hibernate / Spring Data JPA

### Infrastructure & Performance

- **Containerization:** Docker & Docker Compose
- **Traffic Control:** Custom Distributed Rate Limiter (Go implementation)
- **API Testing:** Postman
- **Documentation:** Swagger UI

## System Architecture

The application adheres to a strict Controller-Service-Repository layered architecture, with an upcoming Rate Limiting Layer to protect API resources.

```
graph LR
    A[Mobile Client] -->|HTTP/REST| G[Go Rate Limiter]
    G -->|Allowed Request| B[Spring Boot API]
    B -->|JSON Response| A
    B -->|JPA/Hibernate| C[PostgreSQL Database]
    G -.->|Token Bucket Check| R[Redis Cache]
```

## Authentication Flow

The security module is managed by the AuthService, implementing the following lifecycle:

1. **Validation:** Verifies uniqueness of credentials (Email/Phone).
2. **Encryption:** Hashes sensitive data using BCryptPasswordEncoder.
3. **Persistence:** Transactional storage of User entities.
4. **Token Generation:** Issuance of secure JWTs via JwtUtil.
5. **Response:** Returns authorized session tokens to the client.

## Configuration & Environment Variables

### Secrets Management

For security compliance, sensitive configuration files are excluded from version control. A local configuration file must be created before the application can start.

1. Navigate to the resources directory: `src/main/resources/`
2. Create a file named: `application-secrets.properties`
3. Populate the file with the following keys (obtain values from the repository administrator):

```properties
# Email Configuration (SMTP)
spring.mail.username=admin@example.com
spring.mail.password=secure-app-password

# JWT Configuration (Min 32 characters)
jwt.secret=YOUR_SECURE_256_BIT_SECRET_KEY

# Redis Configuration (Upcoming)
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## Installation & Deployment

### Method 1: Containerized Deployment (Docker)

This is the recommended method for development to ensure environment consistency across Java, Go, and database services.

**Prerequisites:** Docker Desktop installed and running.

**Build and Start:**

```bash
docker-compose up --build
```

This command compiles the code, builds the JAR/Go binaries, and starts Application, Redis, and Database containers.

#### Docker Command Reference:

| Command | Description |
|---------|-------------|
| `docker-compose up` | Starts existing containers. Use for quick startup. |
| `docker-compose up --build` | Recompiles source code and rebuilds containers. |
| `docker-compose down` | Stops and removes containers. |
| `docker-compose down -v` | Stops containers and deletes the database volume (Resets Data). |

### Method 2: Local Deployment (Maven)

Suitable for native debugging without Docker.

**Prerequisites:** Java 17+, PostgreSQL, and Redis installed locally.

**Configure Database:**

Update `src/main/resources/application.properties` with your local credentials.

**Execute Run Command:**

```bash
# Windows
.\mvnw spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

## API Reference

### Authentication Module

**Base Path:** `/auth`

| HTTP Method | Endpoint | Description |
|-------------|----------|-------------|
| POST | `/register` | Register a new Player account. |
| POST | `/login` | Authenticate user and retrieve JWT. |
| POST | `/register/vendor` | Register a new Venue Owner account. |

### Venue Management

**Base Path:** `/api/venues`

| HTTP Method | Endpoint | Description |
|-------------|----------|-------------|
| GET | `/` | Retrieve a paginated list of all venues. |
| POST | `/` | Create a new venue record (Vendor Role required). |

## Development Roadmap

### Phase 1: Core Backend Foundation

- [x] Implementation of Spring Boot & PostgreSQL connectivity.
- [x] User Entity modeling and Repository layer creation.
- [x] Security configuration and JWT utility implementation.
- [x] Development of Authentication Service logic.

### Phase 2: Venue Management System

- [ ] Venue Entity modeling.
- [ ] Implementation of Venue CRUD operations.
- [ ] Booking transaction logic.

### Phase 3: Performance & Scalability (Immediate Priority)

- [ ] Integration of Redis for caching session data and token blocklisting.
- [ ] Development of a Go (Golang) microservice for high-throughput request handling.
- [ ] Implementation of a distributed Rate Limiter middleware to prevent API abuse.

### Phase 4: Client Integration

- [ ] React Native environment initialization.
- [ ] Axios service layer configuration for API integration.

## License

Copyright © 2025 SportsApp Inc. All Rights Reserved.
