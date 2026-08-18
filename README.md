# Portfolio CMS

A minimal, static-first portfolio CMS built with Vanilla JavaScript and Java Spring Boot.

The public portfolio is static and fast, while content can be managed through a unified admin dashboard.

## Features

- Static public portfolio with dynamic editorial sections
- Responsive design with Mac-style single-letter navigation sidebar
- Vanilla HTML, CSS & JavaScript (zero frameworks)
- Spring Boot backend with Spring Security & CSRF protection
- PostgreSQL database support (with embedded H2 for zero-setup local dev)
- Admin CMS (Profile, Dynamic Sections, Projects, Tech Stack, Experience, Achievements, Blog & Media Library)
- Versioned JSON publishing with atomic two-phase commit and rollback safety
- Session-based admin authentication with BCrypt hashing
- Single-server local filesystem publishing & static asset serving
- Per-file resilient fallback content loading
- Dedicated `/blog/<slug>` direct SPA routing

## Architecture

```text
                PUBLIC
                  │
             your-domain
                  │
          HTML / CSS / JS
                  │
          Published JSON
                  │
      Local Static Storage
      (Single-Server Deployment)


                 ADMIN
                  │
          your-domain/admin
                  │
            Admin Frontend
                  │
             Spring Boot
                  │
        PostgreSQL / Dev H2
                  │
         Atomic Publish Engine
                  │
        Versioned JSON Files
```

The backend serves the public frontend, admin dashboard, published JSON data (`../frontend/data/published/default`), and uploaded media assets (`../frontend/media`) from the same deployment.

## Tech Stack

### Frontend

- HTML5
- CSS3 (Vanilla CSS with CSS custom properties)
- Vanilla JavaScript (ES6 Modules & Classes)

### Backend

- Java 21
- Spring Boot 3.2.x
- Spring Security (Session auth, BCrypt, Cookie CSRF)
- Spring Data JPA / Hibernate

### Database

- **Production:** PostgreSQL (automatically detected from JDBC URL)
- **Local Development:** H2 in-memory (zero setup, PostgreSQL compatibility mode)

### Storage

- **Local Filesystem Publishing:** Single-server publishing to `../frontend/data/published/default` and media files to `../frontend/media`.

## Project Structure

```text
portfolio-cms/
├── frontend/     # Public portfolio SPA & published JSON/media
├── admin/        # Admin CMS dashboard
└── backend/      # Spring Boot backend application
```

## Publishing Workflow

Content is stored in the database and published atomically as versioned JSON.

```text
Database Content
       ↓
Generate v(N+1) JSON Files
       ↓
Write Versioned Files to Disk
       ↓
Verify Integrity of All Files
       ↓
Atomic Manifest Update (manifest.json)
       ↓
Public Portfolio (Instant Zero Cold-Start Update)
```

If publishing fails during file generation or writing, the previous published version remains active in `manifest.json`.

## Local Development

### 1. Backend (Spring Boot)

Run locally with the `dev` profile to enable the H2 Web Console and local developer conveniences:

```bash
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

*On Linux/macOS:*
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

When running with the `dev` profile:
- Embedded in-memory H2 database runs automatically with zero configuration.
- H2 Web Console is accessible at `http://localhost:8080/h2-console` (`JDBC URL: jdbc:h2:mem:portfoliodb`).
- Admin UI is available at `http://localhost:8080/admin/`.
- Public Portfolio is available at `http://localhost:8080/`.

### 2. Frontend Standalone Static Testing (Optional)

If running the static frontend via a separate HTTP server (e.g. `npx serve`):

```bash
npx serve frontend -p 3000
```

*Note:* Standalone static servers should be configured with SPA rewrite rules for `/blog/* -> /index.html`.

## Production Configuration

Production builds run with default production-safe settings (H2 console disabled, driver auto-detected from JDBC URL).

### Required Production Environment Variables

| Variable | Description | Example |
| :--- | :--- | :--- |
| `JDBC_DATABASE_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://postgres.example.com:5432/portfoliodb` |
| `JDBC_DATABASE_USERNAME` | Database username | `portfolio_user` |
| `JDBC_DATABASE_PASSWORD` | Database password | `StrongSecretPassword123` |
| `ADMIN_USERNAME` | Initial Admin bootstrap username | `admin` |
| `ADMIN_INITIAL_PASSWORD` | Initial Admin bootstrap password | `SuperSecurePassword!` |

*Optional overrides:*
- `JDBC_DATABASE_DRIVER`: Explicit driver class name (Spring Boot automatically infers PostgreSQL driver from the JDBC URL if omitted).
- `PORT`: Server port (defaults to `8080`).

### Running in Production

```bash
java -jar target/portfolio-backend-1.0.0.jar
```

## License

MIT license
