package buerostack.jwt.repo;

import buerostack.jwt.entity.CustomJwtMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomJwtMetadataRepo extends JpaRepository<CustomJwtMetadata, UUID> {
}