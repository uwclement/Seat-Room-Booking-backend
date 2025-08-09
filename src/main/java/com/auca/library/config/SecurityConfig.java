package com.auca.library.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.auca.library.security.jwt.AuthEntryPointJwt;
import com.auca.library.security.jwt.AuthTokenFilter;
import com.auca.library.security.services.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/auth/signin").permitAll()
                    .requestMatchers("/api/auth/signup").permitAll()
                    .requestMatchers("/api/auth/verify").permitAll()
                    .requestMatchers("/api/auth/check-email").permitAll()
                    .requestMatchers("/api/auth/check-student-id").permitAll()
                    .requestMatchers("/api/auth/check-employee-id").permitAll()
                    .requestMatchers("/api/test/**").permitAll()
                    .requestMatchers("/api/notifications/stream").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("api/scan/**").permitAll()
                    .requestMatchers("api/scan/info").permitAll()
                    .requestMatchers("/api/qr/image/**").permitAll()
                    // Admin-only endpoints
                    .requestMatchers("/api/auth/create-staff").hasRole("ADMIN")
                    .requestMatchers("/api/users/staff/**").hasRole("ADMIN")
                    .requestMatchers("/api/users/admins").hasRole("ADMIN")
                    .requestMatchers("/api/users/equipment-admin").hasRole("ADMIN")
                    .requestMatchers("/api/users/hod").hasRole("ADMIN")
                    // Staff management endpoints
                    .requestMatchers("/api/users/librarians/**").hasAnyRole("ADMIN", "LIBRARIAN")
                    .requestMatchers("/api/users/professors/**").hasAnyRole("ADMIN", "HOD")
                    // Password change endpoint (all authenticated users)
                    .requestMatchers("/api/auth/change-password").hasAnyRole("USER", "ADMIN", "LIBRARIAN", "PROFESSOR", "HOD", "EQUIPMENT_ADMIN")
                    .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        // configuration.setAllowedOrigins(Arrays.asList("http://10.24.229.246:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}