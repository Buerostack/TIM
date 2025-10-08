package buerostack.jwt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtSigningConfig {

    private final JwtSignatureProperties properties;

    public JwtSigningConfig(JwtSignatureProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwtEncoder jwtEncoder() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
        try (InputStream is = new java.io.FileInputStream(properties.getKeyStore().replace("file:", ""))) {
            keyStore.load(is, properties.getKeyStorePassword().toCharArray());
        }

        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(properties.getKeyAlias(), properties.getKeyStorePassword().toCharArray());
        RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(properties.getKeyAlias()).getPublicKey();

        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(properties.getKeyAlias()).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwkSource);
    }
}
