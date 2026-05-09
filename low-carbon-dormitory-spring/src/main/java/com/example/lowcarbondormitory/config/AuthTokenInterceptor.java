package com.example.lowcarbondormitory.config;

import com.example.lowcarbondormitory.common.AuthException;
import com.example.lowcarbondormitory.service.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthTokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String[] allowedRoles = allowedRoles(request.getRequestURI());
        if (allowedRoles.length == 0) {
            return true;
        }

        TokenService.TokenPayload payload = tokenService.verify(resolveBearerToken(request));
        if (!isAllowedRole(payload.role(), allowedRoles)) {
            throw new AuthException("当前登录身份无权访问该接口");
        }

        request.setAttribute("authRole", payload.role());
        request.setAttribute("authSubject", payload.subject());
        request.setAttribute("authClaims", payload.claims());
        return true;
    }

    private String[] allowedRoles(String uri) {
        if (uri.startsWith("/student/") && !"/student/login".equals(uri)) {
            return new String[] {"STUDENT", "ADMIN"};
        }

        if (uri.startsWith("/admin/") && !"/admin/login".equals(uri)) {
            return new String[] {"ADMIN"};
        }

        return new String[0];
    }

    private boolean isAllowedRole(String role, String[] allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new AuthException("请先登录");
        }

        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new AuthException("登录 token 类型无效");
        }

        return authorization.substring(prefix.length()).trim();
    }
}
