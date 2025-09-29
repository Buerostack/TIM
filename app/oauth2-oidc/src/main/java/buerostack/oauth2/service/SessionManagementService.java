package buerostack.oauth2.service;

import buerostack.oauth2.model.AuthSession;
import buerostack.oauth2.model.TokenResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing OAuth2 authentication sessions
 */
@Service
public class SessionManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagementService.class);

    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory session storage (in production, use Redis or database)
    private final Map<String, AuthSession> sessionStorage = new ConcurrentHashMap<>();

    /**
     * Create a new authentication session
     */
    public AuthSession createSession(String providerId, TokenResponse tokenResponse,
                                   JWTClaimsSet idTokenClaims, String ipAddress, String userAgent) {

        String sessionId = generateSessionId();
        String userId = extractUserId(idTokenClaims);

        logger.info("Creating new session {} for user {} with provider {}", sessionId, userId, providerId);

        AuthSession session = new AuthSession(sessionId, userId, providerId);

        // Set expiration (default 24 hours, but limited by token expiration)
        Instant sessionExpiry = Instant.now().plus(24, ChronoUnit.HOURS);
        if (tokenResponse.getExpiresIn() != null) {
            Instant tokenExpiry = Instant.now().plus(tokenResponse.getExpiresIn(), ChronoUnit.SECONDS);
            if (tokenExpiry.isBefore(sessionExpiry)) {
                sessionExpiry = tokenExpiry;
            }
        }
        session.setExpiresAt(sessionExpiry);

        // Store tokens (in production, encrypt these)
        AuthSession.TokenData tokenData = new AuthSession.TokenData();
        tokenData.setAccessToken(tokenResponse.getAccessToken());
        tokenData.setRefreshToken(tokenResponse.getRefreshToken());
        tokenData.setIdToken(tokenResponse.getIdToken());
        if (tokenResponse.getExpiresIn() != null) {
            tokenData.setTokenExpiresAt(Instant.now().plus(tokenResponse.getExpiresIn(), ChronoUnit.SECONDS));
        }
        session.setTokens(tokenData);

        // Store session metadata
        AuthSession.SessionMetadata metadata = new AuthSession.SessionMetadata();
        metadata.setIpAddress(ipAddress);
        metadata.setUserAgent(userAgent);

        // Extract authentication method and level of assurance from claims
        if (idTokenClaims != null) {
            try {
                Object amr = idTokenClaims.getClaim("amr");
                if (amr != null) {
                    metadata.setAuthenticationMethod(amr.toString());
                }

                String acr = idTokenClaims.getStringClaim("acr");
                if (acr != null) {
                    metadata.setLevelOfAssurance(acr);
                }
            } catch (Exception e) {
                logger.warn("Error extracting authentication metadata: {}", e.getMessage());
            }
        }

        session.setSessionMetadata(metadata);

        // Store session
        sessionStorage.put(sessionId, session);

        logger.info("Session {} created successfully, expires at {}", sessionId, session.getExpiresAt());

        return session;
    }

    /**
     * Validate and retrieve session
     */
    public SessionValidationResult validateSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return new SessionValidationResult(false, "Session ID is required", null);
        }

        AuthSession session = sessionStorage.get(sessionId);
        if (session == null) {
            return new SessionValidationResult(false, "Session not found", null);
        }

        // Check if session is expired
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus("expired");
            return new SessionValidationResult(false, "Session has expired", session);
        }

        // Check if session is revoked
        if ("revoked".equals(session.getStatus())) {
            return new SessionValidationResult(false, "Session has been revoked", session);
        }

        // Update last activity
        session.setLastActivity(Instant.now());

        logger.debug("Session {} validated successfully", sessionId);

        return new SessionValidationResult(true, "Session is valid", session);
    }

    /**
     * Revoke/logout session
     */
    public boolean revokeSession(String sessionId, String reason) {
        AuthSession session = sessionStorage.get(sessionId);
        if (session == null) {
            return false;
        }

        session.setStatus("revoked");
        session.setLastActivity(Instant.now());

        logger.info("Session {} revoked. Reason: {}", sessionId, reason != null ? reason : "User logout");

        return true;
    }

    /**
     * Extend session validity
     */
    public boolean extendSession(String sessionId, int additionalMinutes) {
        AuthSession session = sessionStorage.get(sessionId);
        if (session == null || !"active".equals(session.getStatus())) {
            return false;
        }

        Instant newExpiry = session.getExpiresAt().plus(additionalMinutes, ChronoUnit.MINUTES);
        session.setExpiresAt(newExpiry);
        session.setLastActivity(Instant.now());

        logger.info("Session {} extended until {}", sessionId, newExpiry);

        return true;
    }

    /**
     * Get session information
     */
    public AuthSession getSession(String sessionId) {
        return sessionStorage.get(sessionId);
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        int cleanedCount = 0;

        sessionStorage.entrySet().removeIf(entry -> {
            AuthSession session = entry.getValue();
            boolean isExpired = session.getExpiresAt() != null && session.getExpiresAt().isBefore(now);
            if (isExpired) {
                cleanedCount++;
            }
            return isExpired;
        });

        if (cleanedCount > 0) {
            logger.info("Cleaned up {} expired sessions", cleanedCount);
        }
    }

    /**
     * Generate cryptographically secure session ID
     */
    private String generateSessionId() {
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        return "sess_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /**
     * Extract user ID from ID token claims
     */
    private String extractUserId(JWTClaimsSet idTokenClaims) {
        if (idTokenClaims == null) {
            return "unknown_user";
        }

        try {
            String sub = idTokenClaims.getSubject();
            return sub != null ? sub : "unknown_user";
        } catch (Exception e) {
            logger.warn("Error extracting user ID from claims: {}", e.getMessage());
            return "unknown_user";
        }
    }

    // Result classes
    public static class SessionValidationResult {
        private final boolean valid;
        private final String message;
        private final AuthSession session;

        public SessionValidationResult(boolean valid, String message, AuthSession session) {
            this.valid = valid;
            this.message = message;
            this.session = session;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public AuthSession getSession() { return session; }
    }
}