import type { FastifyInstance } from 'fastify';
import type Redis from 'ioredis';
import { logger } from './logger.js';

/**
 * Live-position WebSocket.  Each connection subscribes to its tenant's
 * Redis pub/sub channel and forwards messages.  This avoids every WS replica
 * having to talk to Postgres for fanout.
 *
 * Auth: client connects with `Authorization: Bearer <jwt>` either via header
 * or as a query param.  We validate before opening the upgrade.
 */
export function registerWebSocket(app: FastifyInstance, deps: { redisSub: Redis }) {

  app.get('/ws/positions', { websocket: true }, async (socket, req) => {
    let tenantId: string;
    try {
      // Allow ?token=... for browsers that can't set headers on WS upgrade.
      const token = (req.headers.authorization?.replace('Bearer ', ''))
                  ?? (req.query as { token?: string })?.token;
      if (!token) throw new Error('missing token');
      const payload = await app.jwt.verify<{ tid: string; typ: string }>(token);
      if (payload.typ !== 'access') throw new Error('not an access token');
      tenantId = payload.tid;
    } catch (err) {
      logger.warn({ err }, 'ws auth failed');
      socket.close(4401, 'unauthorized');
      return;
    }

    const channel = `tenant:${tenantId}:positions`;
    const sub = deps.redisSub.duplicate();
    await sub.subscribe(channel);
    sub.on('message', (_ch, msg) => {
      if (socket.readyState === socket.OPEN) socket.send(msg);
    });

    socket.on('close', () => {
      sub.unsubscribe(channel).finally(() => sub.disconnect());
    });
    socket.on('error', (err) => logger.warn({ err }, 'ws error'));
  });
}
