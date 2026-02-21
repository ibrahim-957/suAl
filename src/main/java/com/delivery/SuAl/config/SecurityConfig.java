package com.delivery.SuAl.config;

import com.delivery.SuAl.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/v1/api/auth/otp/send",
                                "/v1/api/auth/otp/verify",
                                "/v1/api/auth/**",
                                "/health/**",
                                "/",
                                "/api/public/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/campaign/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/campaign").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/products").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/categories").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/companies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/companies").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/product-sizes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/product-sizes/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/product-prices/product/*/active").permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/api/affordable-packages/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/affordable-packages/company/**").permitAll()

                        .requestMatchers("/v1/api/cart/**").permitAll()

                        .requestMatchers("/v1/api/promos/validate").permitAll()
                        .requestMatchers("/v1/api/campaign/validate").permitAll()
                        .requestMatchers("/v1/api/campaign/eligible").permitAll()

                        .requestMatchers(HttpMethod.POST, "/v1/api/customers").permitAll()

                        .requestMatchers("/v1/api/payment/callback").permitAll()

                        .requestMatchers("/v1/api/admins/**").hasRole("ADMIN")
                        .requestMatchers("/v1/api/operators/**").hasRole("ADMIN")

                        .requestMatchers("/v1/api/purchase-invoices/*/approve").hasRole("ADMIN")

                        .requestMatchers("/v1/api/warehouse-transfers/*/complete").hasRole("ADMIN")

                        .requestMatchers("/v1/api/warehouses/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/purchase-invoices/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/warehouse-transfers/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/stock-movements/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/product-prices/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/product-sizes/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/affordable-packages/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/promos/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/drivers/orders").hasAnyRole("ADMIN", "DRIVER")
                        .requestMatchers("/v1/api/drivers/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/orders/create-by-operator").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/*/approve").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/operator/*/reject").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/*/assign-driver").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/pending").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/today/count").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/revenue").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/v1/api/orders").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/v1/api/orders/*/complete").hasAnyRole("ADMIN", "DRIVER")
                        .requestMatchers("/v1/api/orders/my-orders").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/orders/create-by-customer").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/orders/customer/*/reject").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/orders/**").hasAnyRole("ADMIN", "CUSTOMER", "OPERATOR", "DRIVER")

                        .requestMatchers("/v1/api/customers/addresses/**").hasAnyRole("ADMIN", "CUSTOMER", "OPERATOR")
                        .requestMatchers("/v1/api/customers/**").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers("/v1/api/package-orders/my-package-orders").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/package-orders/*/cancel").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/package-orders/*/auto-renew").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/package-orders/*/initialize-payment").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/v1/api/package-orders").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/v1/api/package-orders/**").hasAnyRole("ADMIN", "CUSTOMER", "OPERATOR")

                        .requestMatchers("/v1/api/payment/**").hasAnyRole("ADMIN", "CUSTOMER", "OPERATOR")

                        .requestMatchers("/v1/api/notifications/**").authenticated()
                        .requestMatchers("/v1/api/device-tokens/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
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