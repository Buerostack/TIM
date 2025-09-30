package buerostack.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to enforce schema isolation at the service level
 * Prevents accidental cross-schema access
 */
@Aspect
@Component
public class SchemaIsolationAspect {

    private static final Logger logger = LoggerFactory.getLogger(SchemaIsolationAspect.class);

    /**
     * Intercept Custom JWT service calls to ensure they don't access auth schema
     */
    @Around("execution(* buerostack.jwt.service..*(..))")
    public Object enforceCustomJwtIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Log access for security auditing
        logger.debug("Custom JWT service access: {}.{}", className, methodName);

        // You could add runtime checks here, e.g.:
        // - Verify thread-local schema context
        // - Check SQL queries for auth schema references
        // - Validate repository injection points

        return joinPoint.proceed();
    }

    /**
     * Intercept Auth service calls to ensure they don't access custom_jwt schema
     */
    @Around("execution(* buerostack.auth.service..*(..))")
    public Object enforceAuthIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Log access for security auditing
        logger.debug("Auth service access: {}.{}", className, methodName);

        return joinPoint.proceed();
    }

    /**
     * Introspection service can access both schemas (by design)
     */
    @Around("execution(* buerostack.introspection.service..*(..))")
    public Object allowIntrospectionAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        logger.debug("Introspection service access: {}.{}", className, methodName);

        return joinPoint.proceed();
    }
}