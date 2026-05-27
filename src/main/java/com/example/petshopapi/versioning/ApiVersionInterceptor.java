package com.example.petshopapi.versioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Interceptor de versionamento via header X-API-Version.
 *
 * Versões aceitas: 1 (padrão) e 2
 * Versão ausente → assume v1
 * Versão inválida → 400 Bad Request
 * Header X-API-Version é ecoado na resposta
 */
@Slf4j
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final Set<String> SUPPORTED = Set.of("1", "2");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {

        String raw = request.getHeader("X-API-Version");

        if (raw == null || raw.isBlank()) {
            response.setHeader("X-API-Version", "1");
            return true;
        }

        // normaliza: "v1" → "1", "v2" → "2"
        String version = raw.trim().toLowerCase().replace("v", "");

        if (!SUPPORTED.contains(version)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"status\":400,\"erro\":\"Bad Request\"," +
                    "\"mensagem\":\"Versão inválida: '%s'. Versões suportadas: 1, 2.\"}",
                    raw));
            return false;
        }

        log.debug("X-API-Version={} path={}", version, request.getRequestURI());
        response.setHeader("X-API-Version", version);
        return true;
    }
}
