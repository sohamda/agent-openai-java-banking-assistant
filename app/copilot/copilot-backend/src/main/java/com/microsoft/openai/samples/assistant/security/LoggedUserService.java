// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class LoggedUserService {

    public LoggedUser getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return getDefaultUser();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String username =
                    jwt.getClaimAsString("preferred_username") != null
                            ? jwt.getClaimAsString("preferred_username")
                            : jwt.getClaimAsString("upn") != null
                                    ? jwt.getClaimAsString("upn")
                                    : authentication.getName();
            String email =
                    jwt.getClaimAsString("email") != null
                            ? jwt.getClaimAsString("email")
                            : username;
            String displayName =
                    jwt.getClaimAsString("name") != null
                            ? jwt.getClaimAsString("name")
                            : username;
            String role = "user";
            return new LoggedUser(username, email, role, displayName);
        }

        String currentUserName = authentication.getName();
        return new LoggedUser(currentUserName, currentUserName, "user", currentUserName);
    }

    private LoggedUser getDefaultUser() {
        return new LoggedUser(
                "bob.user@contoso.com", "bob.user@contoso.com", "generic", "Bob The User");
    }
}
