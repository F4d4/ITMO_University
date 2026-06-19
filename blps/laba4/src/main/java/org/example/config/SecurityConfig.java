package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/published").permitAll()

                // USER-only operations (upload, edit, publish request, monetization request)
                .requestMatchers(HttpMethod.POST, "/api/videos/upload").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/api/videos/*/info").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/videos/*/submit-publish").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/monetization/request").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/monetization/*/methods").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/videos/drafts").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/videos").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/monetization/my").hasRole("USER")

                // MODERATOR-only operations
                .requestMatchers("/api/moderation/**").hasRole("MODERATOR")

                // Admin operations on users (paginated list)
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()

                // Monetization reads
                .requestMatchers(HttpMethod.GET, "/api/monetization/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*").authenticated()

                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
