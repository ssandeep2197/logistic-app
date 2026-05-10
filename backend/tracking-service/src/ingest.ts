import type { FastifyInstance } from 'fastify';
import type { Pool } from 'pg';
import type { Producer } from 'kafkajs';
import { z } from 'zod';
import { logger } from './logger.js';

const PositionSchema = z.object({
  truckId: z.string().uuid(),
  ts: z.coerce.date().optional(),                 // server clock if omitted
  lat: z.number().gte(-90).lte(90),
  lng: z.number().gte(-180).lte(180),
  headingDeg: z.number().gte(0).lt(360).optional(),
  speedMph: z.number().gte(0).lte(200).optional(),
  altitudeFt: z.number().optional(),
  source: z.enum(['DEVICE','ELD_SAMSARA','ELD_MOTIVE','ELD_GEOTAB','MANUAL','SIMULATED']).default('DEVICE'),
});

const BatchSchema = z.object({ samples: z.array(PositionSchema).max(500) });

/**
 * Ingest endpoint.  Accepts either one sample or a batch.  Writes the row to
 * the hypertable, upserts the latest-position table, then publishes to Kafka
 * for any downstream service that wants to react (dispatch, compliance).
 *
 * Batching saves ~10× on Postgres round-trips when an ELD pushes a buffer.
 */
export function registerIngest(app: FastifyInstance, deps: {
  pool: Pool;
  producer: Producer;
  redisPub: { publish: (channel: string, message: string) => Promise<number> };
}) {

  app.post('/ingest', { preHandler: app.authenticate }, async (req, reply) => {
    // Accept either shape.
    const parsed = req.body && typeof req.body === 'object' && 'samples' in req.body
      ? BatchSchema.safeParse(req.body)
      : BatchSchema.safeParse({ samples: [req.body] });

    if (!parsed.success) {
      return reply.code(422).type('application/problem+json').send({
        type: 'about:blank', title: 'Unprocessable', status: 422,
        code: 'validation_failed', detail: parsed.error.format(),
      });
    }

    const tenantId = req.user.tid;
    const samples = parsed.data.samples.map(s => ({
      ...s, ts: s.ts ?? new Date(),
    }));
    if (samples.length === 0) return reply.code(204).send();

    // Multi-row insert into the hypertable.
    const values: unknown[] = [];
    const tuples: string[] = [];
    samples.forEach((s, i) => {
      const o = i * 9;
      tuples.push(`($${o+1},$${o+2},$${o+3},$${o+4},$${o+5},$${o+6},$${o+7},$${o+8},$${o+9})`);
      values.push(s.ts, tenantId, s.truckId, s.lat, s.lng,
                  s.headingDeg ?? null, s.speedMph ?? null, s.altitudeFt ?? null, s.source);
    });
    const insertSql = `
      INSERT INTO tracking.truck_position
        (ts, tenant_id, truck_id, lat, lng, heading_deg, speed_mph, altitude_ft, source)
      VALUES ${tuples.join(',')}
    `;

    // Upsert latest-position with the newest sample per truck only.  We sort
    // descending so the first hit per truck wins ON CONFLICT DO UPDATE.
    const newest = new Map<string, typeof samples[0]>();
    for (const s of samples) {
      const prev = newest.get(s.truckId);
      if (!prev || prev.ts < s.ts) newest.set(s.truckId, s);
    }
    const lastValues: unknown[] = [];
    const lastTuples: string[] = [];
    [...newest.values()].forEach((s, i) => {
      const o = i * 8;
      lastTuples.push(`($${o+1},$${o+2},$${o+3},$${o+4},$${o+5},$${o+6},$${o+7},$${o+8})`);
      lastValues.push(s.truckId, tenantId, s.ts, s.lat, s.lng, s.headingDeg ?? null, s.speedMph ?? null, null);
    });
    const upsertSql = `
      INSERT INTO tracking.truck_last_position
        (truck_id, tenant_id, ts, lat, lng, heading_deg, speed_mph, state_code)
      VALUES ${lastTuples.join(',')}
      ON CONFLICT (truck_id) DO UPDATE SET
        ts = EXCLUDED.ts, lat = EXCLUDED.lat, lng = EXCLUDED.lng,
        heading_deg = EXCLUDED.heading_deg, speed_mph = EXCLUDED.speed_mph
        WHERE tracking.truck_last_position.ts < EXCLUDED.ts
    `;

    const client = await deps.pool.connect();
    try {
      await client.query('BEGIN');
      await client.query(insertSql, values);
      await client.query(upsertSql, lastValues);
      await client.query('COMMIT');
    } catch (err) {
      await client.query('ROLLBACK');
      logger.error({ err }, 'ingest insert failed');
      return reply.code(500).send({ code: 'persist_failed' });
    } finally {
      client.release();
    }

    // Publish to Kafka + Redis pub/sub for the live map.  Fire-and-forget by
    // design — if Kafka is down the data is already in the DB; a separate
    // catch-up job re-emits unsent rows (TODO: outbox parity with Java side).
    await Promise.allSettled(samples.map(async (s) => {
      const payload = JSON.stringify({
        tenantId, truckId: s.truckId, ts: s.ts.toISOString(),
        lat: s.lat, lng: s.lng, headingDeg: s.headingDeg,
        speedMph: s.speedMph, altitudeFt: s.altitudeFt, source: s.source,
      });
      await deps.producer.send({
        topic: 'truck.position.v1',
        messages: [{ key: s.truckId, value: payload }],
      });
      await deps.redisPub.publish(`tenant:${tenantId}:positions`, payload);
    }));

    return reply.code(202).send({ accepted: samples.length });
  });
}
