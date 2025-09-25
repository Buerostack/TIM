package buerostack;

import buerostack.config.JwtSignerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication(scanBasePackages = {"buerostack"})
public class ServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ServerApplication.class, args);
  }

  @Bean
  public JwtSignerService jwtSignerService(Environment env) {
    String ksPath = firstNonBlank(
        System.getProperty("jwt.signature.key-store"),
        env.getProperty("JWT_SIGNATURE_KEY_STORE"),
        env.getProperty("jwt.signature.key-store"),
        "file:/opt/tim/jwtkeystore.jks"
    );

    String ksType = firstNonBlank(
        System.getProperty("jwt.signature.key-store-type"),
        env.getProperty("JWT_SIGNATURE_KEY_STORE_TYPE"),
        env.getProperty("jwt.signature.key-store-type"),
        "JKS"
    );

    String ksPass = firstNonBlank(
        System.getProperty("jwt.signature.key-store-password"),
        env.getProperty("JWT_SIGNATURE_KEY_STORE_PASSWORD"),
        env.getProperty("jwt.signature.key-store-password"),
        "changeme"
    );

    String alias = firstNonBlank(
        System.getProperty("jwt.signature.key-alias"),
        env.getProperty("JWT_SIGNATURE_KEY_ALIAS"),
        env.getProperty("jwt.signature.key-alias"),
        "jwtsign"
    );

    return new JwtSignerService(ksPath, ksType, ksPass, alias);
  }

  private static String firstNonBlank(String a, String b, String c, String d) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    if (c != null && !c.isBlank()) return c;
    return d;
  }
}
