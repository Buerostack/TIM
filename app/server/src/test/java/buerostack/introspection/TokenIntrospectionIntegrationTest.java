package buerostack.introspection;

import buerostack.introspection.dto.IntrospectionRequest;
import buerostack.introspection.dto.IntrospectionResponse;
import buerostack.introspection.service.TokenIntrospectionService;
import buerostack.jwt.entity.CustomJwtMetadata;
import buerostack.jwt.repo.CustomJwtMetadataRepo;
import buerostack.jwt.service.CustomJwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class TokenIntrospectionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomJwtService customJwtService;

    @MockBean
    private CustomJwtMetadataRepo customJwtMetadataRepo;

    private MockMvc mockMvc;

    private final String VALID_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTYzMDAwMDAwMCwiZXhwIjoxOTQ1MzYwMDAwLCJqdGkiOiJ0ZXN0LWp0aS0xMjM0NTYiLCJpc3MiOiJUSU0iLCJhdWQiOlsidGVzdC1hdWRpZW5jZSJdLCJ0b2tlbl90eXBlIjoiY3VzdG9tX2p3dCJ9.signature";
    private final String REVOKED_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTYzMDAwMDAwMCwiZXhwIjoxOTQ1MzYwMDAwLCJqdGkiOiJyZXZva2VkLWp0aS0xMjM0NTYiLCJpc3MiOiJUSU0iLCJhdWQiOlsidGVzdC1hdWRpZW5jZSJdLCJ0b2tlbl90eXBlIjoiY3VzdG9tX2p3dCJ9.signature";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Should introspect active JWT successfully")
    void testIntrospectActiveJwt() throws Exception {
        // Arrange
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        CustomJwtMetadata metadata = new CustomJwtMetadata(
            jwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(3600),
            jwtId
        );
        metadata.setSubject("testuser");
        metadata.setJwtName("TEST_TOKEN");
        metadata.setIssuer("TIM");
        metadata.setAudience("test-audience");

        when(customJwtService.isRevoked(VALID_JWT)).thenReturn(false);
        when(customJwtMetadataRepo.findCurrentVersionByJwtUuid(jwtId))
            .thenReturn(Optional.of(metadata));

        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken(VALID_JWT);

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value("testuser"))
                .andExpect(jsonPath("$.iss").value("TIM"))
                .andExpect(jsonPath("$.aud").value("test-audience"))
                .andExpect(jsonPath("$.token_type").value("custom_jwt"))
                .andExpect(jsonPath("$.jwt_name").value("TEST_TOKEN"));
    }

    @Test
    @DisplayName("Should introspect revoked JWT as inactive")
    void testIntrospectRevokedJwt() throws Exception {
        // Arrange
        when(customJwtService.isRevoked(REVOKED_JWT)).thenReturn(true);

        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken(REVOKED_JWT);

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Verify no metadata lookup occurred for revoked token
        verify(customJwtMetadataRepo, never()).findCurrentVersionByJwtUuid(any());
    }

    @Test
    @DisplayName("Should handle form-encoded introspection request")
    void testIntrospectFormEncoded() throws Exception {
        // Arrange
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        CustomJwtMetadata metadata = new CustomJwtMetadata(
            jwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(3600),
            jwtId
        );
        metadata.setSubject("testuser");

        when(customJwtService.isRevoked(VALID_JWT)).thenReturn(false);
        when(customJwtMetadataRepo.findCurrentVersionByJwtUuid(jwtId))
            .thenReturn(Optional.of(metadata));

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", VALID_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value("testuser"));
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void testIntrospectEmptyToken() throws Exception {
        // Arrange
        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken("");

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should handle malformed JWT token")
    void testIntrospectMalformedToken() throws Exception {
        // Arrange
        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken("malformed.jwt.token");

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should handle extended JWT in chain")
    void testIntrospectExtendedJwt() throws Exception {
        // Arrange - simulate an extended JWT
        UUID originalJwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID extendedJwtId = UUID.fromString("87654321-4321-4321-4321-210987654321");
        UUID originalMetadataId = UUID.randomUUID();

        // Original token metadata
        CustomJwtMetadata originalMetadata = new CustomJwtMetadata(
            originalJwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(600), // Expired
            originalJwtId
        );
        originalMetadata.setId(originalMetadataId);

        // Extended token metadata
        CustomJwtMetadata extendedMetadata = new CustomJwtMetadata(
            extendedJwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(600),
            Instant.now().plusSeconds(3600), // Still valid
            originalJwtId // Points to original
        );
        extendedMetadata.setSupersedes(originalMetadataId);
        extendedMetadata.setSubject("testuser");
        extendedMetadata.setJwtName("TEST_TOKEN");
        extendedMetadata.setIssuer("TIM");

        String extendedJwtToken = VALID_JWT.replace("test-jti-123456", "extended-jti-789012");

        when(customJwtService.isRevoked(extendedJwtToken)).thenReturn(false);
        when(customJwtMetadataRepo.findCurrentVersionByJwtUuid(extendedJwtId))
            .thenReturn(Optional.of(extendedMetadata));

        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken(extendedJwtToken);

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value("testuser"))
                .andExpect(jsonPath("$.jwt_name").value("TEST_TOKEN"))
                .andExpect(jsonPath("$.original_jwt_uuid").value(originalJwtId.toString()));
    }

    @Test
    @DisplayName("Should handle JWT without metadata")
    void testIntrospectJwtWithoutMetadata() throws Exception {
        // Arrange - JWT that passes signature validation but has no metadata
        UUID jwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");

        when(customJwtService.isRevoked(VALID_JWT)).thenReturn(false);
        when(customJwtMetadataRepo.findCurrentVersionByJwtUuid(jwtId))
            .thenReturn(Optional.empty());

        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken(VALID_JWT);

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value("testuser"))
                .andExpect(jsonPath("$.iss").value("TIM"))
                // Should still return basic JWT claims even without metadata
                .andExpected(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    IntrospectionResponse response = objectMapper.readValue(responseBody, IntrospectionResponse.class);
                    assertNotNull(response.getIat());
                    assertNotNull(response.getExp());
                });
    }

    @Test
    @DisplayName("Should provide extension chain information")
    void testIntrospectWithExtensionChainInfo() throws Exception {
        // Arrange
        UUID originalJwtId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID currentJwtId = UUID.fromString("87654321-4321-4321-4321-210987654321");

        CustomJwtMetadata currentMetadata = new CustomJwtMetadata(
            currentJwtId,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(600),
            Instant.now().plusSeconds(3600),
            originalJwtId // Points to original
        );
        currentMetadata.setSubject("testuser");
        currentMetadata.setJwtName("TEST_TOKEN");
        currentMetadata.setIssuer("TIM");

        // Mock extension chain history
        List<CustomJwtMetadata> extensionChain = List.of(
            createMetadata(originalJwtId, originalJwtId, null),
            createMetadata(UUID.randomUUID(), originalJwtId, UUID.randomUUID()),
            currentMetadata
        );

        when(customJwtService.isRevoked(anyString())).thenReturn(false);
        when(customJwtMetadataRepo.findCurrentVersionByJwtUuid(currentJwtId))
            .thenReturn(Optional.of(currentMetadata));
        when(customJwtMetadataRepo.findAllVersionsByOriginalJwtUuid(originalJwtId))
            .thenReturn(extensionChain);

        String extendedJwtToken = VALID_JWT.replace("test-jti-123456", "extended-jti-789012");
        IntrospectionRequest request = new IntrospectionRequest();
        request.setToken(extendedJwtToken);

        // Act & Assert
        mockMvc.perform(post("/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.extension_count").value(2)) // 2 extensions from original
                .andExpect(jsonPath("$.original_jwt_uuid").value(originalJwtId.toString()));
    }

    private CustomJwtMetadata createMetadata(UUID jwtUuid, UUID originalJwtUuid, UUID supersedes) {
        CustomJwtMetadata metadata = new CustomJwtMetadata(
            jwtUuid,
            "sub,role,iat,exp",
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(3600),
            originalJwtUuid
        );
        if (supersedes != null) {
            metadata.setSupersedes(supersedes);
        }
        return metadata;
    }
}