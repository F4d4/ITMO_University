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
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/videos/published").permitAll()

                .requestMatchers("/engine-rest/**").permitAll()
                .requestMatchers("/app/**").permitAll()
                .requestMatchers("/lib/**").permitAll()
                .requestMatchers("/api/engine/**").permitAll()
                .requestMatchers("/forms/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/videos/upload").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/api/videos/*/info").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/videos/*/start-publish").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/monetization/request").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/monetization/*/start-method").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/videos/drafts").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/videos").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/monetization/my").hasRole("USER")

                .requestMatchers("/api/moderation/**").hasRole("MODERATOR")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("MODERATOR")

                .requestMatchers(HttpMethod.GET, "/api/monetization/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/videos/*").authenticated()

                .requestMatchers("/api/process/**").authenticated()

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
