package com.polygloat.security.api_key_auth;

import com.polygloat.model.ApiKey;
import com.polygloat.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiAuthFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String keyParameter = request.getParameter("ak");

        if (keyParameter != null && !keyParameter.isEmpty()) {
            Optional<ApiKey> ak = apiKeyService.getApiKey(keyParameter);
            if (ak.isPresent()) {
                ApiKeyAuthenticationToken apiKeyAuthenticationToken = new ApiKeyAuthenticationToken(ak.get());
                SecurityContextHolder.getContext().setAuthentication(apiKeyAuthenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/uaa");
    }
}
