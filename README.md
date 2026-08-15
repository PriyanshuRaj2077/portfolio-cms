Portfolio CMS

A minimal, static-first personal portfolio platform with a Vanilla HTML/CSS/JavaScript public frontend, a separate admin CMS, and a Java Spring Boot backend for content management and publishing.

The current deployment is Priyanshu's Portfolio, but the architecture is designed to become a reusable portfolio CMS where identity and content can be managed through the admin panel.

✨ Features

Public Portfolio

Minimal, modern editorial-style interface

Responsive design for mobile, tablet, and desktop

Vanilla HTML, CSS, and JavaScript

Static-first architecture

Fast public loading with no Spring Boot dependency

Dark visual system with restrained orange and purple accents

Large typography-focused hero section

Dynamic portfolio sections

Published content loaded from versioned JSON

Local fallback content when remote content is unavailable

Admin CMS

Separate admin frontend

Session-based authentication

Secure admin-only API

Profile management

Project management

Skills / tech stack management

Experience management

Achievement management

Blog management

Dynamic section management

Section reordering

Section visibility control

Section theme selection

Media management

Draft and publishing workflow

Publishing System

PostgreSQL as the source of truth

Versioned published JSON files

Publication manifest

Atomic publication flow

Previous published version remains active if publishing fails

Persistent object-storage/CDN support

Public frontend does not require Spring Boot to be online

🏗️ Architecture

Public Site

Visitor
   ↓
astra.me
   ↓
Static HTML + CSS + Vanilla JavaScript
   ↓
manifest.json
   ↓
Versioned JSON
   ↓
CDN / Object Storage

Admin & Backend

Admin
   ↓
astra.me/admin
   ↓
Admin Frontend
   ↓
Spring Boot REST API
   ↓
PostgreSQL
   ↓
Publish
   ↓
Object Storage / CDN
   ↓
Versioned JSON
   ↓
astra.me

The public website has zero runtime dependency on Spring Boot.

Spring Boot is only required for administration, database operations, media management, and publishing.

🚀 Why Static-First?

A traditional portfolio might work like:

Visitor
   ↓
Spring Boot
   ↓
Database
   ↓
Portfolio

This means the visitor depends on the backend being available.

This project instead uses:

Visitor
   ↓
Static Frontend
   ↓
Published JSON

Therefore, even if Spring Boot is offline, the public portfolio can continue to work using published content.

📁 Project Structure

portfolio-cms/
│
├── frontend/
│   ├── index.html
│   ├── css/
│   │   ├── variables.css
│   │   ├── global.css
│   │   ├── hero.css
│   │   ├── sections.css
│   │   └── responsive.css
│   ├── js/
│   │   ├── app.js
│   │   ├── config.js
│   │   ├── navigation.js
│   │   └── renderer.js
│   ├── assets/
│   │   ├── images/
│   │   └── icons/
│   └── data/
│       └── published/
│           └── default/
│
├── admin/
│   ├── index.html
│   ├── css/
│   │   └── admin.css
│   └── js/
│       ├── admin-app.js
│       └── api.js
│
└── backend/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            └── resources/

🎨 Public Frontend

The current public portfolio uses a permanent hero followed by CMS-controlled sections.

HERO
  ↓
ACHIEVEMENTS
  ↓
EXPERIENCE
  ↓
TECH STACK
  ↓
PROJECTS
  ↓
BLOG
  ↓
CONTACT

Current hero identity:

PRIYANSHU

Developer • Builder • Curious Mind

The long-term goal is to make the identity fully configurable through the CMS.

🧩 Dynamic Sections

Supported section types include:

TEXT
PROJECTS
SKILLS
TIMELINE
ACHIEVEMENTS
BLOG
GALLERY
CONTACT

A section contains information such as:

id
title
type
sortOrder
visible
theme
contentData

This allows new sections to be added without rewriting the public frontend.

For example:

Photography
Open Source
Research
Currently Learning
Certifications

can be added later through the CMS.

📝 Blog System

Blog posts are managed through the admin CMS.

Workflow:

Admin
  ↓
Create Blog
  ↓
Save Draft
  ↓
Edit
  ↓
Publish
  ↓
Generate Published JSON
  ↓
Public Portfolio

Blog posts can contain:

title

slug

summary

content

publication date

read time

tags

status

Supported statuses:

DRAFT
PUBLISHED

Markdown or formatted content is sanitized before being rendered in the browser to reduce XSS risks.

🛠️ Tech Stack

Public Frontend

HTML5

CSS3

Vanilla JavaScript

JSON

Admin Frontend

HTML5

CSS3

Vanilla JavaScript

REST API

Backend

Java 21

Spring Boot 3.x

Spring Web

Spring Security

Spring Data JPA

Jackson

Database

PostgreSQL

H2 for optional local development

Storage

Local filesystem for development

Persistent object storage / CDN for production

🔐 Security

The admin CMS uses Spring Security with session-based authentication.

Authentication flow:

Admin Login
     ↓
Spring Security
     ↓
Credentials verified
     ↓
Session created
     ↓
HttpOnly session cookie
     ↓
Authenticated Admin

Admin APIs are protected under:

/api/admin/**

Initial admin credentials are supplied through environment variables:

ADMIN_USERNAME
ADMIN_INITIAL_PASSWORD

Passwords are hashed using BCrypt before being stored.

There is no public registration system.

Never commit passwords, API keys, database credentials, or other secrets to the repository.

📦 Publishing Architecture

PostgreSQL is the source of truth.

The public website does not directly query PostgreSQL.

Instead, publishing generates static JSON files.

Example:

profile.v18.json
sections.v18.json
projects.v18.json
skills.v18.json
experience.v18.json
achievements.v18.json
blogs.v18.json

A manifest.json identifies the currently active publication.

Example:

{
  "version": 18,
  "files": {
    "profile": "profile.v18.json",
    "sections": "sections.v18.json",
    "projects": "projects.v18.json",
    "skills": "skills.v18.json",
    "experience": "experience.v18.json",
    "achievements": "achievements.v18.json",
    "blogs": "blogs.v18.json"
  }
}

⚛️ Atomic Publishing

Publishing is designed so visitors never receive a partially published version.

The process is:

1. Read content from PostgreSQL
          ↓
2. Generate new version
          ↓
3. Generate all JSON files
          ↓
4. Upload all files
          ↓
5. Verify all uploads
          ↓
6. Update manifest.json LAST

If any upload fails:

New version
    ↓
Upload failure
    ↓
DO NOT update manifest
    ↓
Previous version remains active

If everything succeeds:

All files uploaded
    ↓
manifest.json updated
    ↓
New version becomes active

The manifest acts as the publication pointer.

🌐 Public Content Loading

The public frontend uses:

PUBLIC_CONTENT_BASE_URL

to locate published content.

The loading process is:

Public Frontend
      ↓
manifest.json
      ↓
Current Version
      ↓
Versioned JSON Files
      ↓
Render Content

Versioned files can be cached because their filenames change whenever the publication version changes.

For example:

projects.v18.json
projects.v19.json

The manifest is refreshed more frequently because it identifies the active version.

🛡️ Fallback Strategy

The public site has a local fallback.

Normal flow:

CDN / Object Storage
        ↓
manifest.json
        ↓
Published Content

If remote content is unavailable:

Remote Content Unavailable
        ↓
Bundled Local JSON
        ↓
Portfolio Still Renders

This prevents a temporary backend/CDN problem from making the portfolio completely unavailable.

⚙️ Local Development

Requirements

Java 21

Maven

Git

Node.js

PostgreSQL for full database testing

H2 may be used for optional local development.

Running the Public Frontend

Use an HTTP server instead of opening index.html directly through file://.

npx serve frontend -p 3000

Then open:

http://localhost:3000

The public frontend can be tested with Spring Boot completely OFF.

Running the Backend

cd backend

Set:

ADMIN_USERNAME
ADMIN_INITIAL_PASSWORD

Configure the database connection for your environment.

Then run:

mvn spring-boot:run

Production secrets should always be supplied through environment variables or a secure secret-management system.

👨‍💻 Admin CMS

The admin interface is available at:

astra.me/admin

The CMS provides management interfaces for:

Dashboard
Profile
Sections
Projects
Skills
Experience
Achievements
Blog
Media

The administrator can update portfolio content without directly editing frontend source files.

🔄 Content Update Workflow

Open /admin
      ↓
Login
      ↓
Edit Content
      ↓
Save Draft
      ↓
Review
      ↓
Publish
      ↓
Generate New JSON Version
      ↓
Upload Published Files
      ↓
Update Manifest
      ↓
Public Site Displays New Content

Example:

Add Project
    ↓
Save
    ↓
PostgreSQL
    ↓
Publish
    ↓
projects.v19.json
    ↓
manifest.json → v19
    ↓
astra.me

🧪 Verification

Public Frontend

Hero renders correctly

PRIYANSHU is visually dominant

Navigation works

Dynamic sections render

JSON loading works

CDN failure fallback works

Mobile layout works

Tablet layout works

Desktop layout works

Admin

Login works

Invalid credentials are rejected

Admin API requires authentication

CRUD operations work

Sections can be reordered

Sections can be hidden/shown

Themes can be changed

Blog drafts work

Media uploads work

Publishing

Draft changes do not immediately change the public site

New versioned JSON files are generated

All files upload successfully

Manifest updates only after successful uploads

Failed publishing keeps the previous manifest

Public site loads the newly published version

🔒 Security Rules

Never commit:

.env
database passwords
ADMIN_INITIAL_PASSWORD
API keys
cloud storage credentials
private credentials

Do not store large image binaries directly in PostgreSQL.

Do not inject unsanitized Markdown/HTML into the DOM.

Do not rely only on frontend authorization.

All admin authorization must be enforced by Spring Security on the backend.

🚧 Current Status

Static public frontend architecture

Responsive portfolio structure

Dynamic JSON content system

Admin CMS architecture

Spring Boot backend

PostgreSQL/JPA persistence

Session-based authentication

Secure admin initialization

Versioned publishing

Atomic manifest publishing

Public fallback content

Admin SPA routing

Future Work

Refine public visual design

Make all identity/profile fields fully CMS-driven

Integrate production object storage/CDN

Deploy public frontend and admin

Add additional section types

Improve blog editing

Perform additional security and performance testing

🎯 Design Philosophy

The project follows one core principle:

Keep the public experience static and fast. Keep content management dynamic.

Visitors should get a fast portfolio without waiting for a backend service.

Administrators should be able to update the portfolio without modifying frontend source code.

This separation keeps the public site simple while allowing the CMS to evolve independently.

🔮 Future Direction

The long-term goal is to make the system completely reusable.

The CMS can eventually control:

Name
Site Title
Tagline
Bio
Profile Image
Social Links
Projects
Skills
Experience
Achievements
Blogs
Sections
Themes

This would allow the same codebase to power different personal portfolios with different identities and content.

📄 License

Choose a license before making the repository public.

MIT License — permissive reuse and modification
