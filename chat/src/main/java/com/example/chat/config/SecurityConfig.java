package com.example.chat.config;

import com.example.chat.jwt.JwtAuthenticationFilter;
import com.example.chat.service.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

   private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

   private final JwtAuthenticationFilter jwtAuthenticationFilter;

   public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
      this.jwtAuthenticationFilter = jwtAuthenticationFilter;
      logger.info("SecurityConfig initialized with JWT filter");
   }

   @Bean
   public SecurityFilterChain configure(HttpSecurity http) throws Exception {
      logger.info("Configuring security filter chain");

      http.csrf(csrf -> csrf.disable())
            .headers(header -> header.frameOptions(frameOptions -> frameOptions.disable()))
            .cors(cors -> cors.configurationSource(addConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/auth/**").permitAll() // Allow all auth endpoints
                  .requestMatchers("/h2-console/**").permitAll()
                  .requestMatchers("/ws/**").permitAll()
                  .requestMatchers("/error").permitAll() // Allow error pages
                  .requestMatchers("/health").permitAll() // Allow health check
                  .requestMatchers("/test").permitAll() // Allow test endpoint
                  .anyRequest().authenticated()) // All other requests require authentication
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

      logger.info("Security filter chain configured successfully");
      return http.build();
   }

   @Bean
   public UserDetailsService userDetailsService() {
      return new CustomUserDetails();
   }

   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   @Bean
   public AuthenticationProvider authenticationProvider() {
      DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
      authProvider.setUserDetailsService(userDetailsService());
      authProvider.setPasswordEncoder(passwordEncoder());
      return authProvider;
   }

   @Bean
   public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
   }

   @Bean
   public CorsConfigurationSource addConfigurationSource() {
      CorsConfiguration config = new CorsConfiguration();
      // Use specific origins instead of wildcard when allowCredentials is true
      config.setAllowedOrigins(
            Arrays.asList("http://localhost:3000", "http://localhost:4200", "http://localhost:8080"));
      config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      config.setAllowedHeaders(Arrays.asList("*"));
      config.setAllowCredentials(false); // Set to false when using wildcard origins or set specific origins
      config.setMaxAge(3600L); // Cache preflight response for 1 hour
      config.addExposedHeader("Authorization"); // Expose Authorization header for CORS

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", config);
      return source;
   }
}
