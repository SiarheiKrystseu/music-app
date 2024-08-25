package com.krystseu.microservices.songservice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class HeaderAuthorizationFilter extends OncePerRequestFilter {

    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rolesHeader = request.getHeader(ROLES_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);

        log.debug("Extracted {}: {}", ROLES_HEADER, rolesHeader);
        log.debug("Extracted {}: {}", USER_ID_HEADER, userId);

        if (rolesHeader == null || rolesHeader.isEmpty()) {
            log.warn("Forbidden: {} header is missing", ROLES_HEADER);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Forbidden: Role is missing");
            return;
        }

        List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
        log.debug("Mapped authorities: {}", authorities);

        Authentication authenticationToken = createAuthenticationToken(request, userId, authorities);
        SecurityContext securityContext = new SecurityContextImpl(authenticationToken);
        SecurityContextHolder.setContext(securityContext);

        log.debug("Authentication set in SecurityContext for userId: {}", userId);

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        return Arrays.stream(rolesHeader.split(","))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                .toList();
    }

    private Authentication createAuthenticationToken(HttpServletRequest request, String userId, List<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }
}


