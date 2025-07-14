package com.example.boardapi.config;

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
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (REST API용)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안씀
                .authorizeHttpRequests(auth -> auth
                        // WebSocket/STOMP endpoints
                        .requestMatchers("/ws-chat/**", "/app/**", "/topic/**", "/auth/refresh").permitAll()

                        .requestMatchers("/api/members/register", "/api/members/login", "/error").permitAll() // 회원가입/로그인
                                                                                                              // 허용
                        .requestMatchers(HttpMethod.PUT, "/api/members/password/reset", "/api/members/password")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards/**", "/api/replies/**",
                                "/api/members/check-nickname", "/api/members/find-id")
                        .permitAll()

                        .requestMatchers("/api/chatrooms/**").authenticated() // 채팅 rest api
                        .requestMatchers(HttpMethod.GET, "/api/members/mypage").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/boards", "/api/replies/**", "/api/members/mypage")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/boards/**", "/api/replies/**", "/api/members/mypage",
                                "/api/members/nickname")
                        .authenticated()

                        .requestMatchers(HttpMethod.DELETE, "/api/boards/**", "/api/replies/**", "/api/members/mypage")
                        .authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.disable()) // 폼로그인 비활성화 (HTML UI 미사용)
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .userDetailsService(userDetailsService) // 사용자 정보 서비스 등록
                // JWT 인증 실패 401 처리
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5174",
                                "http://52.65.37.11",
                                "http://52.65.37.11:5173",
                                "http://serverpro.kro.kr", // 추가
                                "http://www.serverpro.kro.kr", // 선택적으로 www 포함도 고려
                                "http://serverpro.kro.kr:5173") // React 개발 서버 주소
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
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
