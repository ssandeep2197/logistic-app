import Fastify from 'fastify';
import cors from '@fastify/cors';
import websocket from '@fastify/websocket';
import { Kafka } from 'kafkajs';
import Redis from 'ioredis';
import { loadConfig } from './config.js';
import { logger } from './logger.js';
import { createPool, ensureSchema } from './db.js';
import { jwtPlugin } from './auth.js';
import { registerIngest } from './ingest.js';
import { registerWebSocket } from './ws.js';

async function main(): Promise<void> {
  const cfg = loadConfig();

  const app = Fastify({
    logger,
    disableRequestLogging: false,
    bodyLimit: 2 * 1024 * 1024,        // 2 MB — enough for a 500-sample batch.
    trustProxy: true,                  // We sit behind the api-gateway.
  });

  // ── infra wiring ──────────────────────────────────────────────────
  const pool = createPool(cfg.DATABASE_URL);
  await ensureSchema(pool);

  const kafka = new Kafka({
    clientId: 'tracking-service',
    brokers: cfg.KAFKA_BOOTSTRAP.split(','),
  });
  const producer = kafka.producer({ idempotent: true });
  await producer.connect();

  const redisPub = new Redis(cfg.REDIS_URL, { lazyConnect: false });
  const redisSub = new Redis(cfg.REDIS_URL, { lazyConnect: false });

  // ── plugins ──────────────────────────────────────────────────────
  await app.register(cors, {
    origin: (origin, cb) => cb(null, true),    // gateway controls CORS in prod.
    credentials: true,
  });
  await app.register(websocket, { options: { maxPayload: 65_536 } });
  await app.register(jwtPlugin);

  // ── routes ───────────────────────────────────────────────────────
  app.get('/actuator/health', async () => ({ status: 'UP' }));
  app.get('/actuator/health/liveness', async () => ({ status: 'UP' }));
  app.get('/actuator/health/readiness', async () => ({ status: 'UP' }));

  registerIngest(app, { pool, producer, redisPub });
  registerWebSocket(app, { redisSub });

  // ── lifecycle ────────────────────────────────────────────────────
  const shutdown = async (signal: string) => {
    logger.info({ signal }, 'shutting down');
    try {
      await app.close();
      await producer.disconnect();
      await pool.end();
      redisPub.disconnect(); redisSub.disconnect();
    } catch (err) {
      logger.error({ err }, 'shutdown error');
    } finally {
      process.exit(0);
    }
  };
  process.on('SIGTERM', () => void shutdown('SIGTERM'));
  process.on('SIGINT',  () => void shutdown('SIGINT'));

  await app.listen({ host: '0.0.0.0', port: cfg.PORT });
  logger.info({ port: cfg.PORT }, 'tracking-service listening');
}

main().catch((err) => {
  logger.fatal({ err }, 'fatal startup error');
  process.exit(1);
});
