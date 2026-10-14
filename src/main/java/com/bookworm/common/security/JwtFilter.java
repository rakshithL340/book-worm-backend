package com.bookworm.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = resolveToken(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                var authentication = jwtProvider.getAuthentication(jwt);
                if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user from JWT token: {}", authentication.getName());
            } else {
                // Check if request has guest session token header
                String sessionToken = request.getHeader("X-Session-Token");
                if (StringUtils.hasText(sessionToken) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal guestPrincipal = UserPrincipal.createGuest(sessionToken);
                    var guestAuth = new UsernamePasswordAuthenticationToken(
                            guestPrincipal,
                            null,
                            guestPrincipal.getAuthorities()
                    );
                    guestAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(guestAuth);
                    log.debug("Assigned GUEST role for session token: {}", sessionToken);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        return null;
    }
}
