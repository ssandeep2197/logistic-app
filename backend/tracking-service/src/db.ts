import pg from 'pg';
import { logger } from './logger.js';

/**
 * One pooled Postgres connection per service replica.  Hypertable writes use
 * a single multi-row INSERT every batch interval to amortize round-trips.
 */
export function createPool(databaseUrl: string): pg.Pool {
  const pool = new pg.Pool({
    connectionString: databaseUrl,
    max: 20,
    idleTimeoutMillis: 30_000,
    application_name: 'tracking-service',
  });

  pool.on('error', (err) => {
    // Idle clients can be terminated by the DB; just log — pool re-creates them.
    logger.error({ err }, 'pg pool client error');
  });

  return pool;
}

/**
 * Runs the bootstrap DDL idempotently the first time the service starts.
 * Liquibase would be heavier than necessary for a single hypertable + a few
 * derived tables; we keep it inline here.
 */
export async function ensureSchema(pool: pg.Pool): Promise<void> {
  const sql = `
    CREATE SCHEMA IF NOT EXISTS tracking;
    SET search_path TO tracking;

    CREATE TABLE IF NOT EXISTS truck_position (
        ts          TIMESTAMPTZ      NOT NULL,
        tenant_id   UUID             NOT NULL,
        truck_id    UUID             NOT NULL,
        lat         DOUBLE PRECISION NOT NULL,
        lng         DOUBLE PRECISION NOT NULL,
        heading_deg DOUBLE PRECISION,
        speed_mph   DOUBLE PRECISION,
        altitude_ft DOUBLE PRECISION,
        state_code  CHAR(2),
        source      TEXT             NOT NULL DEFAULT 'DEVICE'
    );

    -- Hypertable on the time column. Idempotent thanks to if_not_exists.
    SELECT create_hypertable(
        'tracking.truck_position', 'ts',
        chunk_time_interval => INTERVAL '1 day',
        if_not_exists => TRUE);

    CREATE INDEX IF NOT EXISTS ix_truck_position_truck_ts
        ON tracking.truck_position (truck_id, ts DESC);

    -- Latest-known position per truck — overwritten on every ingest. This is
    -- what the WebSocket fanout reads to populate the live map. ON CONFLICT
    -- keeps it a single round-trip per ingest.
    CREATE TABLE IF NOT EXISTS truck_last_position (
        truck_id    UUID PRIMARY KEY,
        tenant_id   UUID NOT NULL,
        ts          TIMESTAMPTZ NOT NULL,
        lat         DOUBLE PRECISION NOT NULL,
        lng         DOUBLE PRECISION NOT NULL,
        heading_deg DOUBLE PRECISION,
        speed_mph   DOUBLE PRECISION,
        state_code  CHAR(2)
    );
  `;
  await pool.query(sql);
  logger.info('tracking schema ready');
}
