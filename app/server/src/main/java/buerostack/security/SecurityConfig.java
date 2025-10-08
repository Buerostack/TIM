package buerostack.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
  SecurityFilterChain authChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/auth/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  SecurityFilterChain introspectChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/introspect/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  SecurityFilterChain staticChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/", "/*.html", "/*.yaml", "/*.css", "/*.js")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }
}
