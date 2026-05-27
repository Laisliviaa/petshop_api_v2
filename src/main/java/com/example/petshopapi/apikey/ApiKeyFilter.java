package com.example.petshopapi.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(), HttpMethod.DELETE.name()
    );

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase();
        String path   = request.getRequestURI();

        if (!WRITE_METHODS.contains(method)) return true;

        // Geração de chaves é pública
        if (path.startsWith("/api/v1/apikeys") && method.equals("POST")) return true;

        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")
                || path.startsWith("/actuator")
                || method.equals("OPTIONS");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String rawKey = request.getHeader("X-API-Key");
        String method = request.getMethod().toUpperCase();
        String path   = request.getRequestURI();

        if (rawKey == null || rawKey.isBlank()) {
            reject(response, request, 401, "Header X-API-Key ausente. Gere uma chave em POST /api/v1/apikeys.");
            return;
        }

        Optional<ApiKey> opt = apiKeyRepository.findByKeyValueAndActiveTrue(rawKey);
        if (opt.isEmpty()) {
            log.warn("X-API-Key inválida/revogada — path={}", path);
            reject(response, request, 401, "X-API-Key inválida ou revogada.");
            return;
        }

        ApiKey key = opt.get();

        // USER tenta revogar chaves → só ADMIN pode
        if (key.getRole() == ApiKey.AccessLevel.USER
                && method.equals("DELETE")
                && path.startsWith("/api/v1/apikeys/")) {
            reject(response, request, 403, "Apenas chaves com role ADMIN podem revogar outras chaves.");
            return;
        }

        log.info("X-API-Key válida — role={} method={} path={}", key.getRole(), method, path);
        response.setHeader("X-API-Key-Role", key.getRole().name());
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse res, HttpServletRequest req,
                        int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        String error = status == 401 ? "Unauthorized" : "Forbidden";
        res.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status, error, message, req.getRequestURI()));
    }
}
