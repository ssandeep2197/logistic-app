import type { FastifyInstance, FastifyRequest } from 'fastify';
import fp from 'fastify-plugin';
import jwt from '@fastify/jwt';

/**
 * Validates the same JWTs identity-service signs (HS256, shared secret).
 * Populates {@code request.user} with the AuthPrincipal-shaped claims.
 */
export interface JwtPayload {
  sub: string;          // userId
  tid: string;          // tenantId
  eml?: string;
  perm?: string[];      // pre-flattened permission codes
  brn?: string[];       // branch ids
  typ: 'access' | 'refresh';
  iss: string;
  exp: number;
}

declare module '@fastify/jwt' {
  interface FastifyJWT {
    payload: JwtPayload;
    user: JwtPayload;
  }
}

export const jwtPlugin = fp(async (app: FastifyInstance) => {
  await app.register(jwt, {
    secret: process.env.JWT_SIGNING_KEY!,
    verify: { allowedIss: process.env.JWT_ISSUER ?? 'tms-identity' },
    formatUser: (payload) => payload as JwtPayload,
  });

  app.decorate('authenticate', async (request: FastifyRequest) => {
    await request.jwtVerify();
    if (request.user.typ !== 'access') {
      throw new Error('refresh tokens cannot access endpoints');
    }
  });
});

declare module 'fastify' {
  interface FastifyInstance {
    authenticate: (request: FastifyRequest) => Promise<void>;
  }
}
