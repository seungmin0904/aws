package com.example.boardapi.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.boardapi.filter.JwtFilter;
import com.example.boardapi.security.custom.CustomUserDetailsService;
import com.example.boardapi.security.custom.JwtAuthenticationEntryPoint;
import com.example.boardapi.security.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/ws-chat", "/ws-chat/**", "/app/**", "/topic/**", "/auth/refresh").permitAll()

                        .requestMatchers(
                                "/api/members/register",
                                "/api/members/login",
                                "/api/auth/email/send",
                                "/api/auth/email/verify",
                                "/api/auth/email/find-id", "/error")
                        .permitAll()

                        .requestMatchers(HttpMethod.PUT, "/api/members/password/reset", "/api/members/password")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards/**", "/api/replies/**",
                                "/api/members/check-nickname", "/api/members/find-id")
                        .permitAll()
                        .requestMatchers("/api/chatrooms/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/members/mypage").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/boards", "/api/replies/**", "/api/members/mypage")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/boards/**", "/api/replies/**", "/api/members/mypage",
                                "/api/members/nickname")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/boards/**", "/api/replies/**", "/api/members/mypage")
                        .authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .userDetailsService(userDetailsService)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }

    //
    // public AuthenticationManager
    // authenticationManager(AuthenticationConfiguration config) throws Exception {
    // return config.getAuthenticationManager();
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 저장용 - 강력한 인코딩만 사용
    @Bean
    public BCryptPasswordEncoder bcryptEncoder() {
        return new BCryptPasswordEncoder();
    }

    // jwtFilter 주입용
    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter(jwtUtil);
    }

    // 주입용
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

}
