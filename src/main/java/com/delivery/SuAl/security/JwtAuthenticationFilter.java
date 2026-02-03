package com.delivery.SuAl.security;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.repository.OperatorRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final OperatorRepository operatorRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        OperatorContext.clear();

        if (request.getServletPath().contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    if (userDetails instanceof User user) {
                        if (user.getRole() == UserRole.OPERATOR) {
                            setOperatorContext(user);
                        }
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired\",\"message\":\"Please login again\"}");
            return;
        } catch (JwtException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"message\":\"Authentication failed\"}");
            return;
        } catch (Exception ex) {
            log.error("Unexpected authentication error: {}", ex.getMessage(), ex);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            OperatorContext.clear();
        }
    }

    private void setOperatorContext(User user) {
        try {
            operatorRepository.findByUserEmail(user.getEmail())
                    .ifPresent(operator -> {
                        OperatorInfo operatorInfo = OperatorInfo.builder()
                                .operatorId(operator.getId())
                                .companyId(operator.getCompany() != null ? operator.getCompany().getId() : null)
                                .operatorType(operator.getOperatorType())
                                .email(user.getEmail())
                                .firstName(operator.getFirstName())
                                .lastName(operator.getLastName())
                                .build();

                        OperatorContext.setCurrentOperator(operatorInfo);

                        log.debug("Operator context established: {} (Type: {}, Company: {})",
                                operatorInfo.getEmail(),
                                operatorInfo.getOperatorType(),
                                operatorInfo.getCompanyId());
                    });
        } catch (Exception ex) {
            log.error("Failed to set operator context for user: {}", user.getEmail(), ex);
        }
    }
}
