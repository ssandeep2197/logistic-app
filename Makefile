# Logistics App developer shortcuts.
.PHONY: help up down ps logs reset psql redis-cli kafka-ui \
        be-build be-test fe-install fe-dev fe-build clean

help:
	@echo "Common targets:"
	@echo "  make up              Start local infra (postgres, redis, kafka, ...)"
	@echo "  make down            Stop local infra"
	@echo "  make ps              Show infra container status"
	@echo "  make logs S=svc      Tail logs from one infra container"
	@echo "  make reset           Tear down infra AND wipe volumes"
	@echo "  make psql            Open psql in the running postgres container"
	@echo "  make be-build        Build all backend services (Gradle)"
	@echo "  make be-test         Run all backend tests"
	@echo "  make fe-install      pnpm install at the frontend workspace"
	@echo "  make fe-dev          Run shell + all MFEs in dev mode"
	@echo "  make fe-build        Build all MFEs for production"

up:
	docker compose -f infra/docker-compose.yml up -d

down:
	docker compose -f infra/docker-compose.yml down

ps:
	docker compose -f infra/docker-compose.yml ps

logs:
	docker compose -f infra/docker-compose.yml logs -f $(S)

reset:
	docker compose -f infra/docker-compose.yml down -v

psql:
	docker compose -f infra/docker-compose.yml exec postgres psql -U tms tms

redis-cli:
	docker compose -f infra/docker-compose.yml exec redis redis-cli

kafka-ui:
	@echo "Kafka UI: http://localhost:8080"
	@open http://localhost:8080 2>/dev/null || true

be-build:
	cd backend && ./gradlew build -x test

be-test:
	cd backend && ./gradlew test

fe-install:
	cd frontend && pnpm install

fe-dev:
	cd frontend && pnpm dev

fe-build:
	cd frontend && pnpm build

clean:
	cd backend && ./gradlew clean
	cd frontend && pnpm -r clean || true
