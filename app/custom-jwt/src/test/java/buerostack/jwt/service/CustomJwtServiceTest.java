package buerostack.jwt.service;

import buerostack.config.JwtSignerService;
import buerostack.jwt.entity.CustomDenylist;
import buerostack.jwt.entity.CustomJwtMetadata;
import buerostack.jwt.repo.CustomDenylistRepo;
import buerostack.jwt.repo.CustomJwtMetadataRepo;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CustomJwtServiceTest {

    @Mock
    private JwtSignerService jwtSignerService;

    @Mock
    private CustomDenylistRepo denylistRepo;

    @Mock
    private CustomJwtMetadataRepo metadataRepo;

    private CustomJwtService customJwtService;

    private final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTYzMDAwMDAwMCwiZXhwIjoxNjMwMDAzNjAwLCJqdGkiOiJ0ZXN0LWp0aS0xMjM0NTYiLCJpc3MiOiJUSU0iLCJhdWQiOlsidGVzdC1hdWRpZW5jZSJdLCJ0b2tlbl90eXBlIjoiY3VzdG9tX2p3dCJ9";
    private final String EXTENDED_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTYzMDAwMzUwMCwiZXhwIjoxNjMwMDA3MjAwLCJqdGkiOiJleHRlbmRlZC1qdGktNzg5MDEyIiwiaXNzIjoiVElNIiwiYXVkIjpbInRlc3QtYXVkaWVuY2UiXSwidG9rZW5fdHlwZSI6ImN1c3RvbV9qd3QifQ";

    @BeforeEach
    void setUp() {
        customJwtService = new CustomJwtService(jwtSignerService, denylistRepo, metadataRepo);
    }

    @Test
    @DisplayName("Should generate JWT with new schema")
    void testGenerateJwtWithNewSchema() throws Exception {
        // Arrange
        String jwtName = "TEST_TOKEN";
        Map<String, Object> claims = Map.of(
            "sub", "testuser",
            "role", "admin"
        );
        String issuer = "TIM";
        List<String> audiences = List.of("test-audience");
        long ttl = 3600L;

        when(jwtSignerService.sign(any(), eq(issuer), eq(audiences), eq(ttl)))
            .thenReturn(TEST_TOKEN);

        // Act
        String result = customJwtService.generate(jwtName, claims, issuer, audiences, ttl);

        // Assert
        assertEquals(TEST_TOKEN, result);

        // Verify metadata was saved with correct structure
        verify(metadataRepo).save(argThat(metadata -> {
            assertNotNull(metadata.getId(), "ID should be set");
            assertNotNull(metadata.getCreatedAt(), "Created at should be set");
            assertNotNull(metadata.getJwtUuid(), "JWT UUID should be set");
            assertEquals(metadata.getJwtUuid(), metadata.getOriginalJwtUuid(),
                        "For new tokens, original_jwt_uuid should equal jwt_uuid");
            assertNull(metadata.getSupersedes(), "New token should not supersede anything");
            assertEquals(jwtName, metadata.getJwtName());
            assertEquals("testuser", metadata.getSubject());
            assertEquals(issuer, metadata.getIssuer());
            assertTrue(metadata.getClaimKeys().contains("sub"));
            assertTrue(metadata.getClaimKeys().contains("role"));
            return true;
        }));

        // Verify token_type claim was added
        verify(jwtSignerService).sign(argThat(claimsWithType -> {
            Map<String, Object> claimsMap = (Map<String, Object>) claimsWithType;
            assertEquals("custom_jwt", claimsMap.get("token_type"));
            assertEquals("testuser", claimsMap.get("sub"));
            assertEquals("admin", claimsMap.get("role"));
            return true;
        }), eq(issuer), eq(audiences), eq(ttl));
    }

    @Test
    @DisplayName("Should extend JWT with proper chain tracking")
    void testExtendJwtWithChainTracking() throws Exception {
        // Arrange
        UUID originalJwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID originalMetadataId = UUID.randomUUID();

        CustomJwtMetadata existingMetadata = new CustomJwtMetadata(
            originalJwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(600), // Expires soon
            originalJwtId // For original token
        );
        existingMetadata.setId(originalMetadataId);
        existingMetadata.setSubject("testuser");
        existingMetadata.setJwtName("TEST_TOKEN");

        when(jwtSignerService.verify(TEST_TOKEN)).thenReturn(true);
        when(denylistRepo.findById(originalJwtId)).thenReturn(Optional.empty());
        when(metadataRepo.findCurrentVersionByJwtUuid(originalJwtId))
            .thenReturn(Optional.of(existingMetadata));
        when(jwtSignerService.sign(any(), eq("TIM"), anyList(), eq(3600L)))
            .thenReturn(EXTENDED_TOKEN);

        // Act
        String result = customJwtService.extend(TEST_TOKEN, "TIM", List.of("test-audience"), 3600L);

        // Assert
        assertEquals(EXTENDED_TOKEN, result);

        // Verify new metadata was saved with extension chain
        verify(metadataRepo).save(argThat(metadata -> {
            assertNotNull(metadata.getId(), "New metadata should have ID");
            assertNotNull(metadata.getCreatedAt(), "Created at should be set");
            assertEquals(originalJwtId, metadata.getOriginalJwtUuid(),
                        "Should point to original JWT UUID");
            assertEquals(originalMetadataId, metadata.getSupersedes(),
                        "Should supersede the previous version");
            assertNotEquals(originalJwtId, metadata.getJwtUuid(),
                          "Extended token should have new JWT UUID");
            return true;
        }));

        // Verify old token was denylisted
        verify(denylistRepo).save(argThat(denylistEntry -> {
            assertEquals(originalJwtId, denylistEntry.getJwtUuid());
            assertNotNull(denylistEntry.getDenylistedAt());
            return true;
        }));
    }

    @Test
    @DisplayName("Should reject extension of expired token")
    void testRejectExtensionOfExpiredToken() throws Exception {
        // Arrange - create an expired token
        String expiredToken = createExpiredTestToken();

        when(jwtSignerService.verify(expiredToken)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            customJwtService.extend(expiredToken, "TIM", List.of("test-audience"), 3600L);
        });

        assertEquals("Token expired - cannot extend", exception.getMessage());

        // Verify no metadata or denylist operations occurred
        verify(metadataRepo, never()).save(any());
        verify(denylistRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should reject extension of revoked token")
    void testRejectExtensionOfRevokedToken() throws Exception {
        // Arrange
        UUID revokedJwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        when(jwtSignerService.verify(TEST_TOKEN)).thenReturn(true);
        when(denylistRepo.findById(revokedJwtId)).thenReturn(Optional.of(new CustomDenylist()));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            customJwtService.extend(TEST_TOKEN, "TIM", List.of("test-audience"), 3600L);
        });

        assertEquals("Token revoked - cannot extend", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject extension of token with invalid signature")
    void testRejectExtensionOfInvalidToken() throws Exception {
        // Arrange
        when(jwtSignerService.verify(TEST_TOKEN)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            customJwtService.extend(TEST_TOKEN, "TIM", List.of("test-audience"), 3600L);
        });

        assertEquals("Invalid signature - cannot extend", exception.getMessage());
    }

    @Test
    @DisplayName("Should check if token is revoked")
    void testIsRevoked() throws Exception {
        // Arrange
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        // Test revoked token
        when(denylistRepo.findById(jwtId)).thenReturn(Optional.of(new CustomDenylist()));
        assertTrue(customJwtService.isRevoked(TEST_TOKEN));

        // Test non-revoked token
        when(denylistRepo.findById(jwtId)).thenReturn(Optional.empty());
        assertFalse(customJwtService.isRevoked(TEST_TOKEN));
    }

    @Test
    @DisplayName("Should denylist token successfully")
    void testDenylistToken() throws Exception {
        // Arrange
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        String reason = "User logout";

        when(denylistRepo.findById(jwtId)).thenReturn(Optional.empty());

        // Act
        boolean result = customJwtService.denylist(TEST_TOKEN, reason);

        // Assert
        assertTrue(result);
        verify(denylistRepo).save(argThat(denylistEntry -> {
            assertEquals(jwtId, denylistEntry.getJwtUuid());
            assertEquals(reason, denylistEntry.getReason());
            assertNotNull(denylistEntry.getDenylistedAt());
            assertNotNull(denylistEntry.getExpiresAt());
            return true;
        }));
    }

    @Test
    @DisplayName("Should not denylist already revoked token")
    void testDenylistAlreadyRevokedToken() throws Exception {
        // Arrange
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        when(denylistRepo.findById(jwtId)).thenReturn(Optional.of(new CustomDenylist()));

        // Act
        boolean result = customJwtService.denylist(TEST_TOKEN, "reason");

        // Assert
        assertFalse(result);
        verify(denylistRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should handle extension chain scenario")
    void testCompleteExtensionChain() throws Exception {
        // This test simulates a complete extension chain:
        // Original -> Extension 1 -> Extension 2

        UUID originalJwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID extension1Id = UUID.randomUUID();
        UUID extension1JwtId = UUID.fromString("87654321-4321-4321-4321-210987654321");

        // Setup original token metadata
        CustomJwtMetadata originalMetadata = new CustomJwtMetadata(
            originalJwtId, "sub,role,iat,exp",
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(600), // Expired
            originalJwtId
        );
        originalMetadata.setId(UUID.randomUUID());

        // Setup first extension metadata
        CustomJwtMetadata extension1Metadata = new CustomJwtMetadata(
            extension1JwtId, "sub,role,iat,exp",
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(600), // Still valid
            originalJwtId // Points to original
        );
        extension1Metadata.setId(extension1Id);
        extension1Metadata.setSupersedes(originalMetadata.getId());

        when(jwtSignerService.verify(EXTENDED_TOKEN)).thenReturn(true);
        when(denylistRepo.findById(extension1JwtId)).thenReturn(Optional.empty());
        when(metadataRepo.findCurrentVersionByJwtUuid(extension1JwtId))
            .thenReturn(Optional.of(extension1Metadata));
        when(jwtSignerService.sign(any(), eq("TIM"), anyList(), eq(3600L)))
            .thenReturn("second-extension-token");

        // Act - extend the first extension
        String result = customJwtService.extend(EXTENDED_TOKEN, "TIM", List.of("test-audience"), 3600L);

        // Assert
        assertEquals("second-extension-token", result);

        verify(metadataRepo).save(argThat(metadata -> {
            assertEquals(originalJwtId, metadata.getOriginalJwtUuid(),
                        "Should still point to original JWT");
            assertEquals(extension1Id, metadata.getSupersedes(),
                        "Should supersede the first extension");
            return true;
        }));
    }

    private String createExpiredTestToken() {
        // This would normally create a properly formatted JWT with expired timestamp
        // For testing purposes, we'll use a mock that the service recognizes as expired
        return "expired.jwt.token";
    }
}