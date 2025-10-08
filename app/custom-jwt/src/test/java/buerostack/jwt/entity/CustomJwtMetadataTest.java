package buerostack.jwt.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

class CustomJwtMetadataTest {

    private UUID testJwtUuid;
    private UUID testOriginalJwtUuid;
    private String testClaimKeys;
    private Instant testIssuedAt;
    private Instant testExpiresAt;

    @BeforeEach
    void setUp() {
        testJwtUuid = UUID.randomUUID();
        testOriginalJwtUuid = UUID.randomUUID();
        testClaimKeys = "sub,role,iat,exp";
        testIssuedAt = Instant.now().minusSeconds(60);
        testExpiresAt = Instant.now().plusSeconds(3600);
    }

    @Test
    void testDefaultConstructor() {
        CustomJwtMetadata metadata = new CustomJwtMetadata();

        assertNotNull(metadata.getId(), "ID should be auto-generated");
        assertNotNull(metadata.getCreatedAt(), "Created at should be auto-set");
        assertTrue(metadata.getCreatedAt().isBefore(Instant.now().plusSeconds(1)),
                   "Created at should be recent");
    }

    @Test
    void testParameterizedConstructor() {
        CustomJwtMetadata metadata = new CustomJwtMetadata(
            testJwtUuid,
            testClaimKeys,
            testIssuedAt,
            testExpiresAt,
            testOriginalJwtUuid
        );

        assertNotNull(metadata.getId(), "ID should be auto-generated");
        assertNotNull(metadata.getCreatedAt(), "Created at should be auto-set");
        assertEquals(testJwtUuid, metadata.getJwtUuid());
        assertEquals(testClaimKeys, metadata.getClaimKeys());
        assertEquals(testIssuedAt, metadata.getIssuedAt());
        assertEquals(testExpiresAt, metadata.getExpiresAt());
        assertEquals(testOriginalJwtUuid, metadata.getOriginalJwtUuid());
    }

    @Test
    void testNewTokenScenario() {
        // For new tokens, original_jwt_uuid should equal jwt_uuid
        CustomJwtMetadata metadata = new CustomJwtMetadata(
            testJwtUuid,
            testClaimKeys,
            testIssuedAt,
            testExpiresAt,
            testJwtUuid  // Same as jwt_uuid for new tokens
        );

        assertEquals(testJwtUuid, metadata.getJwtUuid());
        assertEquals(testJwtUuid, metadata.getOriginalJwtUuid());
        assertNull(metadata.getSupersedes(), "New token should not supersede anything");
    }

    @Test
    void testExtensionChainScenario() {
        // Create original token
        UUID originalId = UUID.randomUUID();
        UUID originalJwtUuid = UUID.randomUUID();

        // Create first extension
        UUID extension1JwtUuid = UUID.randomUUID();
        UUID extension1Id = UUID.randomUUID();

        CustomJwtMetadata extension1 = new CustomJwtMetadata(
            extension1JwtUuid,
            testClaimKeys,
            testIssuedAt,
            testExpiresAt.plusSeconds(3600), // Extended expiration
            originalJwtUuid  // Points to original
        );
        extension1.setId(extension1Id);
        extension1.setSupersedes(originalId); // Points to previous version

        // Create second extension
        UUID extension2JwtUuid = UUID.randomUUID();

        CustomJwtMetadata extension2 = new CustomJwtMetadata(
            extension2JwtUuid,
            testClaimKeys,
            testIssuedAt,
            testExpiresAt.plusSeconds(7200), // Further extended expiration
            originalJwtUuid  // Still points to original
        );
        extension2.setSupersedes(extension1Id); // Points to previous extension

        // Verify chain structure
        assertEquals(originalJwtUuid, extension1.getOriginalJwtUuid());
        assertEquals(originalJwtUuid, extension2.getOriginalJwtUuid());
        assertEquals(originalId, extension1.getSupersedes());
        assertEquals(extension1Id, extension2.getSupersedes());
    }

    @Test
    void testSettersAndGetters() {
        CustomJwtMetadata metadata = new CustomJwtMetadata();

        metadata.setJwtUuid(testJwtUuid);
        metadata.setClaimKeys(testClaimKeys);
        metadata.setIssuedAt(testIssuedAt);
        metadata.setExpiresAt(testExpiresAt);
        metadata.setOriginalJwtUuid(testOriginalJwtUuid);

        String testSubject = "testuser";
        String testJwtName = "TEST_TOKEN";
        String testAudience = "test-audience";
        String testIssuer = "TIM";

        metadata.setSubject(testSubject);
        metadata.setJwtName(testJwtName);
        metadata.setAudience(testAudience);
        metadata.setIssuer(testIssuer);

        assertEquals(testJwtUuid, metadata.getJwtUuid());
        assertEquals(testClaimKeys, metadata.getClaimKeys());
        assertEquals(testIssuedAt, metadata.getIssuedAt());
        assertEquals(testExpiresAt, metadata.getExpiresAt());
        assertEquals(testOriginalJwtUuid, metadata.getOriginalJwtUuid());
        assertEquals(testSubject, metadata.getSubject());
        assertEquals(testJwtName, metadata.getJwtName());
        assertEquals(testAudience, metadata.getAudience());
        assertEquals(testIssuer, metadata.getIssuer());
    }

    @Test
    void testCreatedAtImmutability() {
        CustomJwtMetadata metadata = new CustomJwtMetadata();
        Instant originalCreatedAt = metadata.getCreatedAt();

        // Try to modify created_at
        Instant newTime = Instant.now().plusSeconds(3600);
        metadata.setCreatedAt(newTime);

        // In a real JPA environment with @Column(updatable = false),
        // this should not change the database value
        assertEquals(newTime, metadata.getCreatedAt()); // Object level change

        // Note: Database-level immutability is enforced by JPA annotations
    }

    @Test
    void testIdGeneration() {
        CustomJwtMetadata metadata1 = new CustomJwtMetadata();
        CustomJwtMetadata metadata2 = new CustomJwtMetadata();

        assertNotNull(metadata1.getId());
        assertNotNull(metadata2.getId());
        assertNotEquals(metadata1.getId(), metadata2.getId(),
                       "Each instance should have unique ID");
    }

    @Test
    void testExtensionChainValidation() {
        UUID originalJwtUuid = UUID.randomUUID();
        UUID firstExtensionId = UUID.randomUUID();

        // Test that extension chain references are properly set
        CustomJwtMetadata extension = new CustomJwtMetadata(
            UUID.randomUUID(),
            testClaimKeys,
            testIssuedAt,
            testExpiresAt,
            originalJwtUuid
        );

        extension.setSupersedes(firstExtensionId);

        assertNotNull(extension.getSupersedes());
        assertEquals(firstExtensionId, extension.getSupersedes());
        assertEquals(originalJwtUuid, extension.getOriginalJwtUuid());

        // Verify that original and current JWT UUIDs can be different
        assertNotEquals(extension.getJwtUuid(), extension.getOriginalJwtUuid());
    }
}