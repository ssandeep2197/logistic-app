import { z } from 'zod';

/**
 * One typed module for every env var the service reads.  Validated at boot —
 * if anything is missing or malformed, the process exits before binding the
 * port, so a misconfigured replica is never marked Ready.
 */
const Schema = z.object({
  PORT: z.coerce.number().int().positive().default(8004),
  LOG_LEVEL: z.enum(['fatal', 'error', 'warn', 'info', 'debug', 'trace']).default('info'),

  DATABASE_URL: z.string().default('postgres://tracking_svc:tracking_dev_password@localhost:5432/tms?options=-c%20search_path%3Dtracking'),
  KAFKA_BOOTSTRAP: z.string().default('localhost:9092'),
  REDIS_URL: z.string().default('redis://localhost:6379'),

  // Same secret as identity-service signs with — we only validate.
  JWT_SIGNING_KEY: z.string().min(32),
  JWT_ISSUER: z.string().default('tms-identity'),

  // GPS sampling: default 30s, with adaptive bursts handled at the producer.
  GPS_TARGET_INTERVAL_MS: z.coerce.number().int().positive().default(30_000),
});

export type Config = z.infer<typeof Schema>;

export function loadConfig(): Config {
  const parsed = Schema.safeParse(process.env);
  if (!parsed.success) {
    // Print every failure, then die — never start with bad config.
    console.error('Invalid configuration:\n' + JSON.stringify(parsed.error.format(), null, 2));
    process.exit(1);
  }
  return parsed.data;
}
