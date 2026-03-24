# 3-Tier Java DevOps Application

This project contains a 3-tier application:

1. Presentation tier: Nginx serving a static web UI.
2. Application tier: Java Spring Boot REST API.
3. Data tier: MySQL database.

## Project Structure

- `frontend/` - Presentation layer
- `backend/` - Java API service
- `docker-compose.yml` - Orchestration for all tiers

## Run With Docker Compose

```bash
docker compose up --build
```

After startup:

- Frontend UI: http://localhost:8080
- Backend API: http://localhost:8081/api/notes

## API Endpoints

- `GET /api/notes` - List notes
- `POST /api/notes` - Create note

Sample request:

```json
{
  "title": "Deploy pipeline",
  "content": "Build, test, and deploy using Docker Compose"
}
```

## Stop Containers

```bash
docker compose down
```
