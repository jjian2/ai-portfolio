package com.jian.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // 관리자 로그인 화면은 누구나 접근 가능
                .requestMatchers(
                    "/admin/login",
                    "/error"
                ).permitAll()

                // 관리자 주소는 ADMIN 권한만 접근 가능
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                // 일반 포트폴리오 화면은 누구나 접근 가능
                .requestMatchers(
                    "/",
                    "/projects",
                    "/projects/**",
                    "/contact",
                    "/api/ai/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/files/**"
                ).permitAll()

                // 그 외 주소는 로그인 필요
                .anyRequest()
                .authenticated()
            )

            .formLogin(form -> form

                // 직접 만들 관리자 로그인 페이지
                .loginPage("/admin/login")

                // 로그인 폼이 전송되는 주소
                .loginProcessingUrl("/admin/login")

                // 로그인 성공 후 이동
                .defaultSuccessUrl("/admin/contacts", true)

                // 로그인 실패 시 이동
                .failureUrl("/admin/login?error")

                .permitAll()
            )

            .logout(logout -> logout

                // 로그아웃 요청 주소
                .logoutUrl("/admin/logout")

                // 로그아웃 성공 후 이동
                .logoutSuccessUrl("/admin/login?logout")

                // 세션 삭제
                .invalidateHttpSession(true)

                // 인증 쿠키 삭제
                .deleteCookies("JSESSIONID")

                .permitAll()
            )

            // 지금은 문의 폼과 관리자 기능 구현을 쉽게 하기 위해 비활성화
            .csrf(csrf -> csrf.disable())

            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.factory
            .PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin1234"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}