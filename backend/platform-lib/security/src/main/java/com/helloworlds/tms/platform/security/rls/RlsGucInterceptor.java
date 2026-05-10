package com.helloworlds.tms.platform.security.rls;

import com.helloworlds.tms.platform.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sets the Postgres GUC {@code app.current_tenant} at the start of every
 * {@code @Transactional} method that runs in a tenant context.  RLS policies
 * on each table read this GUC to enforce row visibility — even a buggy query
 * that forgets {@code WHERE tenant_id = ?} cannot return another tenant's rows.
 * <p>
 * Use {@code SET LOCAL} so the GUC dies at transaction commit; it does not
 * leak to whichever connection the pool hands out next.
 */
@Aspect
public class RlsGucInterceptor {

    @PersistenceContext
    private EntityManager em;

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) "
            + "|| @within(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethod() {}

    @Around("transactionalMethod()")
    public Object setRlsGuc(ProceedingJoinPoint pjp) throws Throwable {
        UUID tenant = TenantContext.currentOrNull();
        if (tenant != null) {
            // SET LOCAL is bound to the current transaction; safe with connection pooling.
            em.createNativeQuery("SELECT set_config('app.current_tenant', :t, true)")
              .setParameter("t", tenant.toString())
              .getSingleResult();
        }
        return pjp.proceed();
    }
}
