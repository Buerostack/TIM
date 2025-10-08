package buerostack.jwt.repo;

import buerostack.jwt.entity.CustomJwtMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJwtMetadataRepo extends JpaRepository<CustomJwtMetadata, UUID> {

    Page<CustomJwtMetadata> findBySubject(String subject, Pageable pageable);

    // Find the current active version of a specific JWT
    @Query(value = "SELECT * FROM custom_jwt.jwt_metadata WHERE jwt_uuid = :jwtUuid " +
           "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<CustomJwtMetadata> findCurrentVersionByJwtUuid(@Param("jwtUuid") UUID jwtUuid);

    // Find all versions of a JWT by original JWT UUID
    @Query(value = "SELECT * FROM custom_jwt.jwt_metadata WHERE original_jwt_uuid = :originalJwtUuid " +
           "ORDER BY created_at ASC", nativeQuery = true)
    List<CustomJwtMetadata> findAllVersionsByOriginalJwtUuid(@Param("originalJwtUuid") UUID originalJwtUuid);

    // Find active JWTs for a subject (latest version of each JWT chain)
    @Query(value = "SELECT DISTINCT ON (jwt_uuid) * FROM custom_jwt.jwt_metadata " +
           "WHERE subject = :subject ORDER BY jwt_uuid, created_at DESC", nativeQuery = true)
    List<CustomJwtMetadata> findActiveJwtsBySubject(@Param("subject") String subject);

    @Query(value = "SELECT * FROM custom_jwt.jwt_metadata m WHERE m.subject = :subject " +
           "AND (:issuedAfter IS NULL OR m.issued_at >= :issuedAfter) " +
           "AND (:issuedBefore IS NULL OR m.issued_at <= :issuedBefore) " +
           "AND (:expiresAfter IS NULL OR m.expires_at >= :expiresAfter) " +
           "AND (:expiresBefore IS NULL OR m.expires_at <= :expiresBefore) " +
           "AND (:jwtName IS NULL OR m.jwt_name = :jwtName) " +
           "ORDER BY m.issued_at DESC",
           countQuery = "SELECT count(*) FROM custom_jwt.jwt_metadata m WHERE m.subject = :subject " +
           "AND (:issuedAfter IS NULL OR m.issued_at >= :issuedAfter) " +
           "AND (:issuedBefore IS NULL OR m.issued_at <= :issuedBefore) " +
           "AND (:expiresAfter IS NULL OR m.expires_at >= :expiresAfter) " +
           "AND (:expiresBefore IS NULL OR m.expires_at <= :expiresBefore) " +
           "AND (:jwtName IS NULL OR m.jwt_name = :jwtName)",
           nativeQuery = true)
    Page<CustomJwtMetadata> findByUserFilters(@Param("subject") String subject,
                                             @Param("issuedAfter") Instant issuedAfter,
                                             @Param("issuedBefore") Instant issuedBefore,
                                             @Param("expiresAfter") Instant expiresAfter,
                                             @Param("expiresBefore") Instant expiresBefore,
                                             @Param("jwtName") String jwtName,
                                             Pageable pageable);
}