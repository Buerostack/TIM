package buerostack.jwt.repo;

import buerostack.jwt.entity.CustomJwtMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface CustomJwtMetadataRepo extends JpaRepository<CustomJwtMetadata, UUID> {

    Page<CustomJwtMetadata> findBySubject(String subject, Pageable pageable);

    @Query(value = "SELECT * FROM custom_jwt.jwt_metadata m WHERE m.subject = :subject " +
           "AND m.is_active = true " +
           "AND (:issuedAfter IS NULL OR m.issued_at >= :issuedAfter) " +
           "AND (:issuedBefore IS NULL OR m.issued_at <= :issuedBefore) " +
           "AND (:expiresAfter IS NULL OR m.expires_at >= :expiresAfter) " +
           "AND (:expiresBefore IS NULL OR m.expires_at <= :expiresBefore) " +
           "AND (:jwtName IS NULL OR m.jwt_name = :jwtName) " +
           "ORDER BY m.issued_at DESC",
           countQuery = "SELECT count(*) FROM custom_jwt.jwt_metadata m WHERE m.subject = :subject " +
           "AND m.is_active = true " +
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