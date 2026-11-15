# Gym CRM – Docker setup

Files: `Dockerfile` (multi-stage, non-root JRE 21 image), `.dockerignore`, `docker-compose.yml` (app + PostgreSQL), `.env.example`.

The app runs with the existing Spring `dev` profile (`application-dev.properties`), which reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the environment. `JWT_SECRET` is also injected at startup. No credentials are in the image. Logs go to stdout (Spring Boot default).
The DB host in `DB_URL` is the Compose service name `db` (`jdbc:postgresql://db:5432/...`), reachable over the `gymcrm-net` network.

## 1. Configure
```powershell
Copy-Item .env.example .env     # then edit the values (.env is git-ignored)
```

## 2. Build and run
```powershell
docker compose build                 # builds image gymcrm-app:1.0
docker compose up -d                 # starts db, then app (waits for db healthcheck)
```

Without Compose (manual equivalent):
```powershell
docker build -t gymcrm-app:1.0 .
docker network create gymcrm-net
docker run -d --name gymcrm-db --network gymcrm-net -e POSTGRES_DB=gymcrm-dev -e POSTGRES_USER=gymcrm_dev -e POSTGRES_PASSWORD=change-me postgres:16-alpine
docker run -d --name gymcrm-app --network gymcrm-net -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev -e DB_URL=jdbc:postgresql://gymcrm-db:5432/gymcrm-dev -e DB_USERNAME=gymcrm_dev -e DB_PASSWORD=change-me -e JWT_SECRET=random-string-of-at-least-32-chars gymcrm-app:1.0
```

## 3. Verify
```powershell
docker compose ps                                   # both should be (healthy)
curl.exe localhost:8080/actuator/health             # {"status":"UP", db: UP ...}
docker logs gymcrm-app                              # or: docker compose logs -f
docker exec -it gymcrm-app sh                       # diagnostic shell

# Store data
curl.exe -X POST localhost:8080/trainees -H "Content-Type: application/json" -d '{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":\"Tbilisi\"}'
# -> returns username + generated password
curl.exe -X POST localhost:8080/auth/login -H "Content-Type: application/json" -d '{\"username\":\"John.Doe\",\"password\":\"<password>\"}'
# Retrieve data (use the returned token)
curl.exe -H "Authorization: Bearer <token>" localhost:8080/trainees/John.Doe
curl.exe -H "Authorization: Bearer <token>" localhost:8080/training-types
```
Swagger UI: http://localhost:8080/swagger-ui.html

## 4. Stop / restart
```powershell
docker compose restart app          # app reconnects to db; data is still there (check HikariPool in logs)
docker compose stop && docker compose start
docker compose down                 # remove containers, keep data volume
docker compose down -v              # also delete database data
```
