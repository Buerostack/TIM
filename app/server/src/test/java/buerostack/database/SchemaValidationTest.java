package buerostack.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {"/schema-test-setup.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SchemaValidationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should validate custom_jwt schema structure")
    void testCustomJwtSchemaStructure() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Validate jwt_metadata table structure
            validateJwtMetadataTable(metaData);

            // Validate denylist table structure
            validateDenylistTable(metaData, "custom_jwt");

            // Validate indexes
            validateIndexes(metaData, "custom_jwt", "jwt_metadata");
        }
    }

    @Test
    @DisplayName("Should validate auth schema structure")
    void testAuthSchemaStructure() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Validate auth.jwt_metadata table
            validateAuthJwtMetadataTable(metaData);

            // Validate auth.denylist table
            validateDenylistTable(metaData, "auth");

            // Validate auth.oauth_state table
            validateOAuthStateTable(metaData);
        }
    }

    @Test
    @DisplayName("Should validate created_at columns have default values")
    void testCreatedAtDefaults() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check custom_jwt.jwt_metadata.created_at
            validateColumnDefault(metaData, "custom_jwt", "jwt_metadata", "created_at", "now()");

            // Check custom_jwt.denylist.created_at
            validateColumnDefault(metaData, "custom_jwt", "denylist", "created_at", "now()");

            // Check auth.jwt_metadata.created_at
            validateColumnDefault(metaData, "auth", "jwt_metadata", "created_at", "now()");

            // Check auth.denylist.created_at
            validateColumnDefault(metaData, "auth", "denylist", "created_at", "now()");

            // Check auth.oauth_state.created_at
            validateColumnDefault(metaData, "auth", "oauth_state", "created_at", "now()");
        }
    }

    @Test
    @DisplayName("Should validate primary key structure")
    void testPrimaryKeyStructure() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // custom_jwt.jwt_metadata should have 'id' as PK
            validatePrimaryKey(metaData, "custom_jwt", "jwt_metadata", "id");

            // Other tables should have expected PKs
            validatePrimaryKey(metaData, "custom_jwt", "denylist", "jwt_uuid");
            validatePrimaryKey(metaData, "auth", "jwt_metadata", "jwt_uuid");
            validatePrimaryKey(metaData, "auth", "denylist", "jwt_uuid");
            validatePrimaryKey(metaData, "auth", "oauth_state", "state");
        }
    }

    @Test
    @DisplayName("Should validate extension chain foreign key relationships")
    void testExtensionChainReferences() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Verify that supersedes and original_jwt_uuid columns exist
            assertTrue(columnExists(metaData, "custom_jwt", "jwt_metadata", "supersedes"));
            assertTrue(columnExists(metaData, "custom_jwt", "jwt_metadata", "original_jwt_uuid"));

            // Verify column types
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "supersedes", "uuid");
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "original_jwt_uuid", "uuid");

            // Verify nullable constraints
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "supersedes", true);
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "original_jwt_uuid", false);
        }
    }

    @Test
    @DisplayName("Should validate required indexes for performance")
    void testPerformanceIndexes() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Critical indexes for extension chain queries
            assertTrue(indexExists(metaData, "custom_jwt", "jwt_metadata", "idx_custom_jwt_metadata_jwt_uuid"));
            assertTrue(indexExists(metaData, "custom_jwt", "jwt_metadata", "idx_custom_jwt_metadata_original"));
            assertTrue(indexExists(metaData, "custom_jwt", "jwt_metadata", "idx_custom_jwt_metadata_subject"));
            assertTrue(indexExists(metaData, "custom_jwt", "jwt_metadata", "idx_custom_jwt_metadata_issued"));

            // Cleanup indexes
            assertTrue(indexExists(metaData, "custom_jwt", "denylist", "idx_custom_jwt_denylist_exp"));
            assertTrue(indexExists(metaData, "auth", "denylist", "idx_auth_denylist_exp"));
        }
    }

    @Test
    @DisplayName("Should validate data types and constraints")
    void testDataTypesAndConstraints() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // UUID columns
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "id", "uuid");
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "jwt_uuid", "uuid");

            // Timestamp columns
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "created_at", "timestamp");
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "issued_at", "timestamp");
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "expires_at", "timestamp");

            // Text columns
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "claim_keys", "text");
            validateColumnType(metaData, "custom_jwt", "jwt_metadata", "subject", "text");

            // Nullable constraints for required fields
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "id", false);
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "jwt_uuid", false);
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "created_at", false);
            validateColumnNullable(metaData, "custom_jwt", "jwt_metadata", "claim_keys", false);
        }
    }

    private void validateJwtMetadataTable(DatabaseMetaData metaData) throws SQLException {
        String[] expectedColumns = {
            "id", "jwt_uuid", "created_at", "claim_keys", "issued_at",
            "expires_at", "subject", "jwt_name", "audience", "issuer",
            "supersedes", "original_jwt_uuid"
        };

        for (String column : expectedColumns) {
            assertTrue(columnExists(metaData, "custom_jwt", "jwt_metadata", column),
                      "Column " + column + " should exist in custom_jwt.jwt_metadata");
        }
    }

    private void validateDenylistTable(DatabaseMetaData metaData, String schema) throws SQLException {
        String[] expectedColumns = {"jwt_uuid", "created_at", "denylisted_at", "expires_at", "reason"};

        for (String column : expectedColumns) {
            assertTrue(columnExists(metaData, schema, "denylist", column),
                      "Column " + column + " should exist in " + schema + ".denylist");
        }
    }

    private void validateAuthJwtMetadataTable(DatabaseMetaData metaData) throws SQLException {
        String[] expectedColumns = {"jwt_uuid", "created_at", "claim_keys", "issued_at", "expires_at"};

        for (String column : expectedColumns) {
            assertTrue(columnExists(metaData, "auth", "jwt_metadata", column),
                      "Column " + column + " should exist in auth.jwt_metadata");
        }
    }

    private void validateOAuthStateTable(DatabaseMetaData metaData) throws SQLException {
        String[] expectedColumns = {"state", "created_at", "pkce_verifier"};

        for (String column : expectedColumns) {
            assertTrue(columnExists(metaData, "auth", "oauth_state", column),
                      "Column " + column + " should exist in auth.oauth_state");
        }
    }

    private boolean columnExists(DatabaseMetaData metaData, String schema, String table, String column) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, schema, table, column)) {
            return rs.next();
        }
    }

    private boolean indexExists(DatabaseMetaData metaData, String schema, String table, String indexName) throws SQLException {
        try (ResultSet rs = metaData.getIndexInfo(null, schema, table, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (indexName.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateColumnType(DatabaseMetaData metaData, String schema, String table, String column, String expectedType) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, schema, table, column)) {
            assertTrue(rs.next(), "Column " + column + " should exist");
            String actualType = rs.getString("TYPE_NAME").toLowerCase();
            assertTrue(actualType.contains(expectedType.toLowerCase()),
                      "Column " + column + " should be of type " + expectedType + " but was " + actualType);
        }
    }

    private void validateColumnNullable(DatabaseMetaData metaData, String schema, String table, String column, boolean shouldBeNullable) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, schema, table, column)) {
            assertTrue(rs.next(), "Column " + column + " should exist");
            int nullable = rs.getInt("NULLABLE");
            boolean isNullable = (nullable == DatabaseMetaData.columnNullable);
            assertEquals(shouldBeNullable, isNullable,
                        "Column " + column + " nullable constraint mismatch");
        }
    }

    private void validateColumnDefault(DatabaseMetaData metaData, String schema, String table, String column, String expectedDefault) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, schema, table, column)) {
            assertTrue(rs.next(), "Column " + column + " should exist");
            String defaultValue = rs.getString("COLUMN_DEF");
            if (expectedDefault != null) {
                assertNotNull(defaultValue, "Column " + column + " should have a default value");
                assertTrue(defaultValue.toLowerCase().contains(expectedDefault.toLowerCase()),
                          "Column " + column + " should have default '" + expectedDefault + "' but was '" + defaultValue + "'");
            }
        }
    }

    private void validatePrimaryKey(DatabaseMetaData metaData, String schema, String table, String expectedPkColumn) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(null, schema, table)) {
            assertTrue(rs.next(), "Table " + schema + "." + table + " should have a primary key");
            String actualPkColumn = rs.getString("COLUMN_NAME");
            assertEquals(expectedPkColumn, actualPkColumn,
                        "Primary key column mismatch for " + schema + "." + table);
        }
    }

    private void validateIndexes(DatabaseMetaData metaData, String schema, String table) throws SQLException {
        Set<String> foundIndexes = new HashSet<>();
        try (ResultSet rs = metaData.getIndexInfo(null, schema, table, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null && !indexName.endsWith("_pkey")) { // Skip primary key indexes
                    foundIndexes.add(indexName);
                }
            }
        }

        // Verify expected indexes exist
        String[] expectedIndexes = {
            "idx_custom_jwt_metadata_subject",
            "idx_custom_jwt_metadata_issued",
            "idx_custom_jwt_metadata_jwt_uuid",
            "idx_custom_jwt_metadata_original"
        };

        for (String expectedIndex : expectedIndexes) {
            assertTrue(foundIndexes.contains(expectedIndex),
                      "Index " + expectedIndex + " should exist on " + schema + "." + table);
        }
    }
}