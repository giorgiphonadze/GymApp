package com.gymcrm.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final VerifiedToken token;

    public JwtAuthenticationToken(VerifiedToken token) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + token.user().userType().name())));
        this.token = token;
        setAuthenticated(true);
    }

    public VerifiedToken getToken() {
        return token;
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return token.user();
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public String getName() {
        return token.user().username();
    }
}
