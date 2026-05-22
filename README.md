# LTD Works API

LTD Works API is a Spring Boot backend for a book and writings platform (Libroty). It provides user authentication, JWT-based sessions, password recovery, book search with filters, and protected book-addition workflows.

This was my first collaborative project (Last Commit 2023). The repository documents not only the API structure, but also the stack and architectural decisions used while learning how to build a larger service together.

Unfortunately, the project was not completed due to changes in team availability before development reached its final stages. See also: [Frontend](https://github.com/Zemchick1/LTD-Can-Front-New).

Note: The content below was generated with AI assistance.

## What Was Used

- Java 17
- Spring Boot 3, Web, Security, Data JPA
- PostgreSQL
- JDBC / NamedParameterJdbcTemplate for custom search queries
- JWT with `io.jsonwebtoken`
- Jakarta Bean Validation / Hibernate Validator
- Lombok
- Resilience4j rate limiter
- Bucket4j global request rate limiting
- Spring Mail / JavaMail
- Docker and Docker Compose
- Elasticsearch, Logstash, Kibana, and Metricbeat for indexing/monitoring infrastructure
- JUnit 5 and Spring Security Test
- GitHub Actions CI
- SonarQube plugin configuration

## Main Features

- User registration and login
- Email verification token flow
- JWT access tokens stored in HTTP-only cookies
- Refresh tokens stored and validated in PostgreSQL
- Token revocation on logout and password change
- Forgot-password token flow
- Global and endpoint-specific rate limiting
- Book search with dynamic filters
- Filter counts for authors, genres, and tags
- Book addition flow with role-based behavior
- Scheduled cleanup for expired or revoked tokens
- Docker Compose infrastructure for backend, Elasticsearch, Logstash, Kibana, and Metricbeat

## Architecture

The project follows a layered Spring Boot architecture:

```text
src/main/java/com/LTD/ltdWorksAPI
|-- config        # Security, authentication filter, rate limiter, scheduling, app beans
|-- controller    # REST API endpoints
|-- exception     # Global exception handling and custom exceptions
|-- model
|   |-- dto        # Request and response DTOs
|   `-- entity     # JPA entities and API response records
|-- repository    # Spring Data JPA repositories
|-- service       # Business logic and database query orchestration
`-- utils/enums   # Roles, filter statuses, and search categories
```

### Request Flow

1. A client sends an HTTP request to one of the `/v1/...` endpoints.
2. `SecurityConfiguration` applies CORS, disables server-side sessions, and registers the custom filters.
3. `GlobalRateLimiterFilter` limits traffic per client IP with Bucket4j.
4. `AuthenticationFilter` reads `jwt_token` and `refresh_token` cookies for protected endpoints.
5. Controllers validate the request shape and delegate to services.
6. Services use repositories or custom SQL through `JdbcTemplate` / `NamedParameterJdbcTemplate`.
7. Errors are normalized by `GlobalExceptionHandler` into consistent API error responses.

## Core Modules

### Authentication

Authentication is handled through `AuthController`, `AuthService`, `JWTService`, `LogoutService`, and `HelperFunctionsService`.

The API creates two cookies after registration or login:

- `jwt_token` - short-lived JWT access token
- `refresh_token` - long-lived refresh token persisted in PostgreSQL

Access tokens are signed with the configured JWT secret. Refresh tokens are stored in `users.refresh_token` and are rotated when `/v1/auth/renew_token` is called.

### Search

Search is handled by `WritingsController` and `WritingsService`.

At the moment, the API search is not yet connected to Elasticsearch. The current implementation builds SQL dynamically for the selected writing category and executes it against PostgreSQL through `NamedParameterJdbcTemplate`. Elasticsearch is already configured in the project, but the backend integration has not been completed.

### Book Addition

Book addition is handled by `BooksAdditionController` and `BooksAdditionService`.

Users with `Admin` or `Moderator` roles can save a book directly through the repository. Regular users go through a request-style path, which is currently logged and left as an extension point.

The addition search endpoint also uses PostgreSQL through `NamedParameterJdbcTemplate`. It supports lookup categories from `BooksAdditionSearchCategory`:

- `AUTHOR`
- `TAG`
- `SERIES`

### Security

Security is configured in `SecurityConfiguration`.

- `/v1/auth/**` and `/v1/search/**` are allowed without Spring Security authentication.
- Some auth routes still expect token cookies inside their service or logout logic.
- Other endpoints require authentication.
- Sessions are stateless.
- CORS allows `http://localhost:3000`.
- Logout is handled through `/v1/auth/logout`.
- `AuthenticationFilter` validates JWT cookies and places the authenticated user into the Spring Security context.
- `GlobalRateLimiterFilter` allows 100 requests per minute per remote address.
- `renew_token` also uses a Resilience4j limiter configured in `application.properties`.

### Persistence

The application uses PostgreSQL through two approaches:

- Spring Data JPA repositories for users, books, and token entities.
- Raw SQL through `JdbcTemplate` for advanced search/filter queries and token revocation updates.

The current code expects database schemas such as:

- `users`
- `writings`

Important tables referenced by the code include:

- `users.user_cred`
- `users.refresh_token`
- `users.jwt_access_token`
- `users.confirmation_token`
- `users.forgot_password_token`
- `writings.book`

Search queries use PostgreSQL trigram functions, so the database should have the `pg_trgm` extension enabled.

### Elasticsearch and Monitoring Infrastructure

`compose.yaml` starts an Elastic-based support stack.

- Elasticsearch for indexing synced book data
- Kibana for visual inspection
- Logstash for syncing PostgreSQL book data into the `book_sync_idx` index
- Metricbeat for Elasticsearch, Kibana, and Docker metrics

`logstash/cfg/logstash.conf` reads from `writings.book` using JDBC and tracks `modification_time` for incremental syncs.

There is a TODO in `BooksAdditionController` for future Elastic usage, but the current Java code does not query Elasticsearch directly.

## API Endpoints

| Method | Endpoint | Description | Auth |
| --- | --- | --- | --- |
| `POST` | `/v1/auth/register` | Register a user, create tokens, and send verification email | Public |
| `POST` | `/v1/auth/login` | Authenticate user and create token cookies | Public |
| `POST` | `/v1/auth/renew_token` | Rotate refresh token and issue a new JWT | Public route, cookie-based |
| `POST` | `/v1/auth/change_password` | Change password and revoke existing tokens | Public route, cookie-based |
| `GET` | `/v1/auth/verification?token=...` | Verify email confirmation token | Public |
| `GET` | `/v1/auth/forgot_password?email=...` | Send forgot-password email | Public |
| `POST` | `/v1/auth/forgot_password/check?token=...` | Check forgot-password token | Public |
| `POST` | `/v1/auth/forgot_password/handle` | Reset password with token | Public |
| `POST` | `/v1/auth/logout` | Revoke current tokens and clear cookies | Public route, cookie-based |
| `POST` | `/v1/search` | Search writings with filters | Public |
| `POST` | `/v1/book/addition/add` | Add or request a new book | Required |
| `POST` | `/v1/book/addition/search` | Search values for book addition fields | Required |
| `GET` | `/v1/demo-controller` | Secured demo endpoint | Required |

## Environment Variables

The application reads sensitive configuration from environment variables:

```env
DATASOURCE_URL=jdbc:postgresql://localhost:5432/your_database
DATABASE_USERNAME=your_database_user
DATABASE_PASSWORD=your_database_password
SECRET_KEY_JWT=base64_encoded_jwt_secret
```

Docker Compose also expects:

```env
ELASTIC_PASSWORD=your_elastic_password
KIBANA_PASSWORD=your_kibana_password
```

Email sending is implemented through `JavaMailSender`, but SMTP host and credentials still need to be configured before real verification and password-reset emails can be delivered.

## Running Locally

Set the required environment variables first.

PowerShell example:

```powershell
$env:DATASOURCE_URL="jdbc:postgresql://localhost:5432/your_database"
$env:DATABASE_USERNAME="your_database_user"
$env:DATABASE_PASSWORD="your_database_password"
$env:SECRET_KEY_JWT="base64_encoded_jwt_secret"
```

Run the application:

```powershell
.\gradlew bootRun
```

The API will start on:

```text
http://localhost:8080
```

## Running With Docker Compose

Build the Spring Boot jar first:

```powershell
.\gradlew bootJar
```

Then start the stack:

```powershell
docker compose up --build
```

Main ports:

- Backend: `http://localhost:8080`
- Elasticsearch: `https://localhost:9200`
- Kibana: `http://localhost:5601`

## Testing

Run tests with:

```powershell
.\gradlew test
```

Run the full Gradle check:

```powershell
.\gradlew check
```

Generate JaCoCo reports:

```powershell
.\gradlew jacocoTestReport
```

## CI/CD

The GitHub Actions workflow in `.github/workflows/git-ci.yml` contains jobs for:

- building the Spring Boot jar
- running tests
- building and pushing the Docker image
- building the Docker Compose stack

SonarQube configuration is present in `build.gradle`, and a SonarQube workflow section exists as a commented extension point.

## Notes

- Database migrations are not included in this repository, so the PostgreSQL schemas and tables must exist before running the backend.
- The Docker and Elastic setup is part of the project infrastructure, while the Spring Boot application remains the main backend service.
- Several TODO comments in the code mark areas planned for future improvement, such as validation hardening, email configuration, pagination, and possible Elasticsearch-backed search.
