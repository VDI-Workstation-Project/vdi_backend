package com.hmws.global.authentication;

import com.hmws.citrix.storefront.session_mgmt.service.StoreFrontLogInService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final TokenProvider tokenProvider;
    private final StoreFrontLogInService storeFrontLogInService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // OPTIONS 요청 처리 추가 필요
        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                "Content-Type, Accept, Authorization, Csrf-Token, X-Citrix-IsUsingHTTPS");

        // OPTIONS 요청 처리 (preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String token = tokenProvider.resolveToken(request);

        try {
            if (token != null && tokenProvider.validateToken(token)) {
                Claims claims = tokenProvider.getClaims(token);

                // Citrix 세션 유효성 검사
                boolean sessionValid = storeFrontLogInService.keepAliveSession(
                        (String) claims.get("citrixSessionId"),
                        (String) claims.get("citrixCsrfToken")
                );

                if (sessionValid) {
                    Authentication authentication = tokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Session expired");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
