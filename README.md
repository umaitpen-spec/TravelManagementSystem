# Travel Management System

A production-grade Spring Boot travel management platform with MVC architecture, modular service layers, and a mobile-responsive Bootstrap frontend.

## New workspace layout (segregated frontend/backend)

- `backend/` — Spring Boot application (Maven module)
  - `backend/pom.xml` — module POM (Spring Boot parent)
  - `backend/src/main/java` — Java sources, controllers, services, repositories, models
  - `backend/src/main/resources/application.properties` — backend configuration

- `frontend/` — UI assets and static pages
  - `frontend/templates/` — HTML pages (static; can be served independently)
  - `frontend/static/` — `css/` and `js/` assets

- Root `pom.xml` is an aggregator POM that builds the `backend` module.

Note: Templates and static assets were moved to `frontend/` to separate UI from API. If you prefer the backend to continue serving the UI via Thymeleaf, copy `frontend/templates` and `frontend/static` into `backend/src/main/resources/` before building.

## Quick run instructions

From repository root (build backend module):

```bash
mvn -T 1C -f backend/pom.xml clean package
mvn -f backend/pom.xml spring-boot:run
```

To enable OpenRouter AI itinerary generation, set your API key before running the backend:

```bash
export OPENROUTER_API_KEY="your-openrouter-key"
mvn -f backend/pom.xml spring-boot:run
```

Optional model override:

```bash
export OPENROUTER_MODEL="nvidia/nemotron-3.5-content-safety:free"
```

Or build then run the jar:

```bash
mvn -T 1C -f backend/pom.xml clean package
java -jar backend/target/*.jar
```

Serve the frontend (static files) separately for development (simple Python server):

```bash
python3 -m http.server 8000 --directory frontend
# then open http://localhost:8000/templates/dashboard.html
```

If you want the backend to serve the frontend pages automatically (same behavior as before), sync files into backend resources:

```bash
./scripts/sync-frontend-to-backend.sh
mvn -f backend/pom.xml spring-boot:run
```

Or use the root aggregator to build:

```bash
mvn -T 1C clean install
```

## Notes & next steps

- Current setup separates UI and API; adjust as needed depending on deployment strategy.
- If you want, I can:
  - Add a small script to sync frontend → backend resources.
  - Convert the project to a full multi-module setup including a `frontend` module (e.g., Node or static packaging).

## Database schema

See `DATABASE_SCHEMA.md` for the MySQL table definitions and relationships.

## API endpoints

- `POST /auth/register` — register user
- `POST /auth/login` — authenticate user
- `POST /api/trips` — create trip
- `GET /api/trips/{id}` — get trip details
- `GET /api/trips/owner/{ownerId}` — list user trips
- `GET /api/trips/{id}/itinerary` — generate AI itinerary using OpenRouter when `OPENROUTER_API_KEY` is set
- `POST /api/trips/{tripId}/bookings` — add booking
- `GET /api/trips/{tripId}/bookings` — list trip bookings
- `POST /api/trips/{tripId}/expenses` — add expense
- `GET /api/trips/{tripId}/expenses` — list trip expenses
- `GET /api/trips/{tripId}/expenses/analysis` — get budget analysis

## Extension plan

- Add JWT auth and Spring Security flows
- Add repository query filters and pagination
- Add hotel and flight service integrations
- Add user profile and permissions management
- Store generated AI itineraries in the database for reuse
# TravelManagementSystem
