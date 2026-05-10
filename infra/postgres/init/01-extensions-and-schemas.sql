-- Initial Postgres bootstrap. Runs once on the first container start.
-- Creates extensions and per-service schemas.  Each microservice manages its own
-- tables inside its schema via Liquibase; this file only creates the namespaces.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "timescaledb";

-- One schema per backend service. Cross-schema queries are NOT allowed in
-- application code — services talk via REST or Kafka events.  Schemas live in
-- the same physical cluster only for cost reasons in v1.
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS operations;
CREATE SCHEMA IF NOT EXISTS dispatch;
CREATE SCHEMA IF NOT EXISTS tracking;
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS payroll;
CREATE SCHEMA IF NOT EXISTS compliance;
CREATE SCHEMA IF NOT EXISTS documents;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS notification;

-- Per-service DB roles. Each service connects with its own role and is GRANTed
-- USAGE only on its own schema, so even a SQL injection bug can't reach another
-- service's data.  Passwords are dev-only — prod uses sealed secrets.
DO $$
DECLARE
    svc TEXT;
    services TEXT[] := ARRAY['identity','operations','dispatch','tracking',
                              'finance','payroll','compliance','documents',
                              'reporting','notification'];
BEGIN
    FOREACH svc IN ARRAY services LOOP
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = svc || '_svc') THEN
            EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', svc || '_svc', svc || '_dev_password');
        END IF;
        EXECUTE format('GRANT USAGE, CREATE ON SCHEMA %I TO %I', svc, svc || '_svc');
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO %I', svc, svc || '_svc');
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO %I', svc, svc || '_svc');
    END LOOP;
END $$;
