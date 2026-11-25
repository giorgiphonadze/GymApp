package com.gymcrm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}. Requests without a Bearer token
 * continue unauthenticated, so the authorization rules decide whether they are allowed.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = JwtService.TOKEN_TYPE + " ";

    private final JwtService jwtService;
    private final TokenDenylist tokenDenylist;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, TokenDenylist tokenDenylist,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.tokenDenylist = tokenDenylist;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            VerifiedToken token = jwtService.verify(header.substring(BEARER_PREFIX.length()).trim());
            if (tokenDenylist.isRevoked(token.id())) {
                throw new InvalidTokenException("Token has been revoked");
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new JwtAuthenticationToken(token));
            SecurityContextHolder.setContext(context);
        } catch (InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, e);
            return;
        }
        chain.doFilter(request, response);
    }
}
