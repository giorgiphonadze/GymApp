package com.gymcrm.config;

import com.gymcrm.security.GymSecurityProperties;
import com.gymcrm.security.JwtAuthenticationFilter;
import com.gymcrm.security.JwtService;
import com.gymcrm.security.RestAccessDeniedHandler;
import com.gymcrm.security.RestAuthenticationEntryPoint;
import com.gymcrm.security.TokenDenylist;
import com.gymcrm.security.TokenRevokingLogoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(GymSecurityProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS_AND_MONITORING = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/h2-console/**", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   TokenDenylist tokenDenylist,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   TokenRevokingLogoutHandler logoutHandler) throws Exception {
        http
                // Stateless Bearer token API: no cookies or sessions, so CSRF protection does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/trainees", "/trainers").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(PUBLIC_DOCS_AND_MONITORING).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/auth/logout"))
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler(logoutHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, tokenDenylist, authenticationEntryPoint),
                        LogoutFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt generates a random salt per password and stores it inside the hash.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(GymSecurityProperties properties) {
        GymSecurityProperties.Cors cors = properties.cors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(cors.allowedMethods());
        configuration.setAllowedHeaders(cors.allowedHeaders());
        configuration.setExposedHeaders(cors.exposedHeaders());
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(cors.maxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
