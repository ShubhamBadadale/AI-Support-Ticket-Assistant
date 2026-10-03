package com.example.tickets.config;

import com.example.tickets.security.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/", "/assets/**", "/index.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**").authenticated()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login").successHandler((req, res, a) -> {
                    res.setContentType("application/json");
                    res.getWriter().write("{\"username\":\"" + a.getName() + "\"}");
                }).failureHandler((req, res, e) -> {
                    res.setStatus(401); res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Invalid credentials\"}");
                }).permitAll())
            .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req, res, a) -> {
                res.setContentType("application/json"); res.getWriter().write("{\"ok\":true}");
            }))
            .sessionManagement(s -> s.maximumSessions(10));
        return http.build();
    }
}
