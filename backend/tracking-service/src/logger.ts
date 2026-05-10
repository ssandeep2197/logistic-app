import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info',
  // Pretty in dev (TTY), JSON in prod — auto-detected.
  transport: process.stdout.isTTY
    ? { target: 'pino-pretty', options: { colorize: true, translateTime: 'SYS:HH:MM:ss.l' } }
    : undefined,
  base: {
    service: 'tracking-service',
    pid: process.pid,
  },
  // Auto-redact sensitive paths so accidentally logging a request doesn't leak tokens.
  redact: ['req.headers.authorization', 'req.headers.cookie', '*.password', '*.passwordHash'],
});
