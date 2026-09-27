# TrulyHealthy — Electronic Health Record (EHR) System

A full-stack EHR built with **Java 17 + Maven + JDBC + MySQL** on the backend
(`com.sun.net.httpserver`, no framework) and **plain HTML/CSS/JavaScript** on
the frontend, in the same style as the rock&rollEvents project — just a
different domain and color theme (royal blue).

Four role-based logins:

| Role | Can do |
|---|---|
| **Patient** | Book, cancel, reschedule appointments; view own medical history; edit own profile |
| **Doctor** | Confirm/complete/cancel appointments; view and add patient history, diagnosis, treatment, prescriptions |
| **Admin** | Create/deactivate/delete Doctor, Admin, and Research accounts; view every appointment system-wide |
| **Research / Government official** | Read-only, **anonymized** aggregate statistics only — no patient names, no individual records |

## Project layout

```
trulyhealthy/
├── pom.xml
├── sql/schema.sql                 # run this first
├── src/main/resources/db.properties
├── src/main/java/com/trulyhealthy/
│   ├── Main.java                  # starts the HTTP server on :8080
│   ├── config/DBConfig.java
│   ├── util/  (PasswordUtil, JsonUtil)
│   ├── model/ (User, Patient, Doctor, Appointment, MedicalRecord)
│   ├── dao/   (JDBC data access, one class per table)
│   └── handler/ (one HttpHandler per REST resource)
└── webapp/                        # static frontend, served by the same Java server
    ├── index.html / register.html / about.html
    ├── patient-dashboard.html / doctor-dashboard.html
    ├── admin-dashboard.html / research-dashboard.html
    ├── css/style.css
    └── js/ (api.js + one file per dashboard)
```

## Setup

1. **Create the database:**
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
   This creates the `trulyhealthy` database, all tables, and demo accounts.

2. **Configure credentials** in `src/main/resources/db.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/trulyhealthy?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
   db.user=root
   db.password=your_mysql_password
   ```
   (The `allowPublicKeyRetrieval=true` flag is required for MySQL 8's default
   `caching_sha2_password` auth plugin.)

3. **Build and run:**
   ```bash
   mvn clean package
   java -jar target/trulyhealthy.jar
   ```
   Run this from the project root, so the `webapp/` folder next to the jar
   is found. Open **http://localhost:8080**.

## Demo credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Doctor | `doctor1` (Dr. Priya Sharma, Cardiology) | `Doctor@123` |
| Doctor | `doctor2` (Dr. Arjun Mehta, General Medicine) | `Doctor@123` |
| Patient | `patient1` (Rahul Verma) | `Patient@123` |
| Patient | `patient2` (Sneha Iyer) | `Patient@123` |
| Research/Gov | `research1` (Ministry of Health) | `Research@123` |

New patients can also self-register from the login page.

## API overview

All endpoints are under `/api`. Every route except `/auth/login` and
`/auth/register` requires an `Authorization: Bearer <token>` header, obtained
from a successful login.

- `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout`, `GET /api/auth/me`
- `GET /api/doctors`, `GET /api/doctors/{id}`, `PUT /api/doctors/{id}`
- `GET /api/patients`, `GET /api/patients/{id}`, `PUT /api/patients/{id}`
- `GET /api/patients/{id}/records`, `POST /api/patients/{id}/records` (doctor only)
- `GET /api/appointments`, `POST /api/appointments`
- `PUT /api/appointments/{id}/cancel|reschedule|confirm|complete`
- `GET /api/admin/users`, `POST /api/admin/users`, `PUT /api/admin/users/{id}/status`, `DELETE /api/admin/users/{id}`
- `GET /api/research/stats` — anonymized counts and breakdowns only

## Known limitations (worth stating up front for a viva)

- Passwords are hashed with **salted SHA-256**, not a dedicated password
  hashing algorithm like bcrypt/argon2 — a deliberate simplification to avoid
  a heavier native dependency in a portfolio project.
- Sessions are plain server-side tokens stored in a `sessions` table, with no
  expiry or refresh mechanism.
- Appointment conflict-checking compares exact timestamps rather than being
  duration-aware (no notion of a doctor's slot length).
- No email/SMS notifications on booking, cancellation, or reschedule.
- The Research/Government dashboard intentionally exposes **aggregate counts
  only** (gender/blood-group/specialization/diagnosis breakdowns) — it never
  queries patient names, contacts, or free-text notes, by design rather than
  by accident.
- Single MySQL instance, no connection pooling (a fresh `DriverManager`
  connection per request) — fine for a demo/portfolio load, not for
  production scale.

## Created by

**Aurko Chatterjee** — see the in-app **About** page (`about.html`) for links.
