package buerostack.jwt.repo;

import buerostack.jwt.entity.CustomJwtMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DataJpaTest
@ActiveProfiles("test")
class CustomJwtMetadataRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomJwtMetadataRepo repository;

    private UUID originalJwtUuid;
    private UUID extension1JwtUuid;
    private UUID extension2JwtUuid;
    private CustomJwtMetadata originalToken;
    private CustomJwtMetadata extension1;
    private CustomJwtMetadata extension2;
    private String testSubject;

    @BeforeEach
    void setUp() {
        originalJwtUuid = UUID.randomUUID();
        extension1JwtUuid = UUID.randomUUID();
        extension2JwtUuid = UUID.randomUUID();
        testSubject = "testuser";

        Instant baseTime = Instant.now();

        // Create original token
        originalToken = new CustomJwtMetadata(
            originalJwtUuid,
            "sub,role,iat,exp",
            baseTime,
            baseTime.plusSeconds(3600),
            originalJwtUuid  // For new tokens, original_jwt_uuid = jwt_uuid
        );
        originalToken.setSubject(testSubject);
        originalToken.setJwtName("ORIGINAL_TOKEN");
        originalToken.setIssuer("TIM");

        // Create first extension (extends original)
        extension1 = new CustomJwtMetadata(
            extension1JwtUuid,
            "sub,role,iat,exp",
            baseTime.plusSeconds(3500), // Near expiration of original
            baseTime.plusSeconds(7200), // Extended 2 hours
            originalJwtUuid  // Points to original
        );
        extension1.setSubject(testSubject);
        extension1.setJwtName("ORIGINAL_TOKEN");
        extension1.setIssuer("TIM");

        // Create second extension (extends first extension)
        extension2 = new CustomJwtMetadata(
            extension2JwtUuid,
            "sub,role,iat,exp",
            baseTime.plusSeconds(7100), // Near expiration of first extension
            baseTime.plusSeconds(10800), // Extended 3 hours
            originalJwtUuid  // Still points to original
        );
        extension2.setSubject(testSubject);
        extension2.setJwtName("ORIGINAL_TOKEN");
        extension2.setIssuer("TIM");

        // Persist and set up chain relationships
        originalToken = entityManager.persistAndFlush(originalToken);
        extension1.setSupersedes(originalToken.getId());
        extension1 = entityManager.persistAndFlush(extension1);
        extension2.setSupersedes(extension1.getId());
        extension2 = entityManager.persistAndFlush(extension2);

        entityManager.clear();
    }

    @Test
    @DisplayName("Should find current version by JWT UUID")
    void testFindCurrentVersionByJwtUuid() {
        // Test finding current version of original JWT
        Optional<CustomJwtMetadata> current = repository.findCurrentVersionByJwtUuid(originalJwtUuid);
        assertTrue(current.isPresent());
        assertEquals(originalToken.getId(), current.get().getId());

        // Test finding current version of first extension
        Optional<CustomJwtMetadata> currentExt1 = repository.findCurrentVersionByJwtUuid(extension1JwtUuid);
        assertTrue(currentExt1.isPresent());
        assertEquals(extension1.getId(), currentExt1.get().getId());

        // Test finding current version of second extension
        Optional<CustomJwtMetadata> currentExt2 = repository.findCurrentVersionByJwtUuid(extension2JwtUuid);
        assertTrue(currentExt2.isPresent());
        assertEquals(extension2.getId(), currentExt2.get().getId());
    }

    @Test
    @DisplayName("Should find all versions by original JWT UUID")
    void testFindAllVersionsByOriginalJwtUuid() {
        List<CustomJwtMetadata> versions = repository.findAllVersionsByOriginalJwtUuid(originalJwtUuid);

        assertEquals(3, versions.size(), "Should find all 3 versions");

        // Verify order (by created_at ASC)
        assertEquals(originalToken.getId(), versions.get(0).getId());
        assertEquals(extension1.getId(), versions.get(1).getId());
        assertEquals(extension2.getId(), versions.get(2).getId());

        // Verify all point to same original
        versions.forEach(version ->
            assertEquals(originalJwtUuid, version.getOriginalJwtUuid())
        );
    }

    @Test
    @DisplayName("Should find active JWTs by subject")
    void testFindActiveJwtsBySubject() {
        // Create another JWT chain for the same subject
        UUID anotherOriginalJwtUuid = UUID.randomUUID();
        CustomJwtMetadata anotherToken = new CustomJwtMetadata(
            anotherOriginalJwtUuid,
            "sub,role,iat,exp",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            anotherOriginalJwtUuid
        );
        anotherToken.setSubject(testSubject);
        anotherToken.setJwtName("ANOTHER_TOKEN");
        anotherToken.setIssuer("TIM");
        entityManager.persistAndFlush(anotherToken);

        List<CustomJwtMetadata> activeTokens = repository.findActiveJwtsBySubject(testSubject);

        // Should return latest version of each JWT chain
        assertEquals(4, activeTokens.size(), "Should find 4 distinct JWT UUIDs");

        // Verify we get the latest version of each JWT UUID
        boolean foundOriginal = false;
        boolean foundExtension1 = false;
        boolean foundExtension2 = false;
        boolean foundAnother = false;

        for (CustomJwtMetadata token : activeTokens) {
            if (token.getJwtUuid().equals(originalJwtUuid)) {
                assertEquals(originalToken.getId(), token.getId());
                foundOriginal = true;
            } else if (token.getJwtUuid().equals(extension1JwtUuid)) {
                assertEquals(extension1.getId(), token.getId());
                foundExtension1 = true;
            } else if (token.getJwtUuid().equals(extension2JwtUuid)) {
                assertEquals(extension2.getId(), token.getId());
                foundExtension2 = true;
            } else if (token.getJwtUuid().equals(anotherOriginalJwtUuid)) {
                assertEquals(anotherToken.getId(), token.getId());
                foundAnother = true;
            }
        }

        assertTrue(foundOriginal && foundExtension1 && foundExtension2 && foundAnother,
                  "Should find all distinct JWT UUIDs");
    }

    @Test
    @DisplayName("Should handle non-existent JWT UUID")
    void testFindCurrentVersionByNonExistentJwtUuid() {
        UUID nonExistentUuid = UUID.randomUUID();
        Optional<CustomJwtMetadata> result = repository.findCurrentVersionByJwtUuid(nonExistentUuid);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle empty results for unknown subject")
    void testFindActiveJwtsByUnknownSubject() {
        List<CustomJwtMetadata> activeTokens = repository.findActiveJwtsBySubject("unknown-user");
        assertTrue(activeTokens.isEmpty());
    }

    @Test
    @DisplayName("Should respect created_at ordering in extension chain")
    void testCreatedAtOrdering() {
        List<CustomJwtMetadata> versions = repository.findAllVersionsByOriginalJwtUuid(originalJwtUuid);

        // Verify chronological order
        for (int i = 1; i < versions.size(); i++) {
            Instant current = versions.get(i).getCreatedAt();
            Instant previous = versions.get(i - 1).getCreatedAt();
            assertTrue(current.isAfter(previous) || current.equals(previous),
                      "Versions should be ordered by created_at ASC");
        }
    }

    @Test
    @DisplayName("Should find by subject with pagination")
    void testFindBySubjectPagination() {
        // This tests the existing Spring Data method with Pageable
        List<CustomJwtMetadata> allForSubject = repository.findBySubject(
            testSubject,
            org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();

        assertFalse(allForSubject.isEmpty());
        allForSubject.forEach(token ->
            assertEquals(testSubject, token.getSubject())
        );
    }

    @Test
    @DisplayName("Should verify extension chain integrity")
    void testExtensionChainIntegrity() {
        // Verify the chain structure is correct
        Optional<CustomJwtMetadata> ext1 = repository.findCurrentVersionByJwtUuid(extension1JwtUuid);
        Optional<CustomJwtMetadata> ext2 = repository.findCurrentVersionByJwtUuid(extension2JwtUuid);

        assertTrue(ext1.isPresent());
        assertTrue(ext2.isPresent());

        // Extension 1 should supersede original
        assertEquals(originalToken.getId(), ext1.get().getSupersedes());

        // Extension 2 should supersede extension 1
        assertEquals(extension1.getId(), ext2.get().getSupersedes());

        // Both extensions should point to the same original
        assertEquals(originalJwtUuid, ext1.get().getOriginalJwtUuid());
        assertEquals(originalJwtUuid, ext2.get().getOriginalJwtUuid());
    }
}