package com.gymcrm.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Logout for stateless JWT authentication: the presented token is put on the denylist so it can no
 * longer be used, even though it has not expired yet.
 */
@Component
public class TokenRevokingLogoutHandler implements LogoutHandler, LogoutSuccessHandler {
    private final TokenDenylist tokenDenylist;

    public TokenRevokingLogoutHandler(TokenDenylist tokenDenylist) {
        this.tokenDenylist = tokenDenylist;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            tokenDenylist.revoke(jwtAuthentication.getToken());
        }
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            RestAuthenticationEntryPoint.write(response, RestAuthenticationEntryPoint.AUTHENTICATION_REQUIRED);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
