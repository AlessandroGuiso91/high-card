package it.sara.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Identity-side configuration: in-memory user store, password encoder and the
 * authentication manager wired by Spring Boot's auto-configuration. Replace the
 * in-memory store with a database-backed {@link UserDetailsService} for production.
 */
@Configuration
public class UsersConfig {

    /**
     * BCrypt with default strength (10). Used to hash passwords stored by the
     * {@link InMemoryUserDetailsManager} and to verify them during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Demo user store: two users with different roles, hashed via BCrypt at startup.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails alice = User.builder()
                .username("alice")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();
        UserDetails bob = User.builder()
                .username("bob")
                .password(encoder.encode("user123"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(alice, bob);
    }

    /**
     * Authentication manager exposed so the {@code AuthController} can authenticate
     * login requests. Spring Boot auto-configures it from the existing
     * {@link UserDetailsService} and {@link PasswordEncoder} beans.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
