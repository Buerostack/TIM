package buerostack.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain jwtChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/jwt/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  SecurityFilterChain taraChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> clients,
      Environment env
  ) throws Exception {

    boolean enabled = env.getProperty("tara.oidc.enabled", Boolean.class, false)
        && clients.getIfAvailable() != null;

    http.securityMatcher("/tara/**")
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());

    if (enabled) {
      http.oauth2Login(Customizer.withDefaults());
    } else {
      http.csrf(csrf -> csrf.disable());
    }
    return http.build();
  }
}
