# Portfolio CMS

A minimal, static-first portfolio CMS built with Vanilla JavaScript and Java Spring Boot.

The public portfolio is static and fast, while content can be managed through a separate admin dashboard.

## Features

- Static public portfolio
- Responsive design
- Vanilla HTML, CSS & JavaScript
- Spring Boot backend
- PostgreSQL database
- Admin CMS
- Project, skill, experience & blog management
- Dynamic sections
- Versioned JSON publishing
- Atomic publishing with rollback safety
- Session-based admin authentication
- CDN/object-storage support
- Local fallback content

## Architecture

```text
                PUBLIC
                  │
               astra.me
                  │
          HTML / CSS / JS
                  │
          Published JSON
                  │
             CDN / Storage


                 ADMIN
                  │
           astra.me/admin
                  │
            Admin Frontend
                  │
             Spring Boot
                  │
              PostgreSQL
                  │
               Publish
                  │
          Versioned JSON
```

## Tech Stack

### Frontend

- HTML5
- CSS3
- Vanilla JavaScript

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA

### Database

- PostgreSQL
- H2 (development)

### Storage

- Object Storage / CDN

## Project Structure

```text
portfolio-cms/
├── frontend/     # Public portfolio
├── admin/        # Admin CMS
└── backend/      # Spring Boot backend
```

## Publishing

Content is stored in PostgreSQL and published as versioned JSON.

```text
Database
   ↓
Generate vN
   ↓
Upload files
   ↓
Verify uploads
   ↓
Update manifest
   ↓
Public website
```

If publishing fails, the previous published version remains active.

## Local Development

### Frontend

```bash
npx serve frontend -p 3000
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

Required environment variables:

```text
ADMIN_USERNAME
ADMIN_INITIAL_PASSWORD
```

## Current Status

🚧 Actively developing.

## License

MIT license
