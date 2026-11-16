package com.mtms.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Servlet filter intercepting mutating requests carrying header Idempotency-Key.
 * Replays cached responses with header 'Idempotent-Replayed: true' or records new executions.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

    private final IdempotencyService idempotencyService;
    private final CurrentActorProvider currentActorProvider;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;
    private final HandlerExceptionResolver exceptionResolver;

    public IdempotencyFilter(
            @Autowired(required = false) IdempotencyService idempotencyService,
            @Autowired(required = false) CurrentActorProvider currentActorProvider,
            ObjectMapper objectMapper,
            @Autowired(required = false) @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            @Autowired(required = false) @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.idempotencyService = idempotencyService;
        this.currentActorProvider = currentActorProvider;
        this.objectMapper = objectMapper;
        this.handlerMapping = handlerMapping;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (idempotencyService == null || currentActorProvider == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = extractIdempotencyKey(request);
        Idempotent idempotentAnnotation = findIdempotentAnnotation(request);

        // 1. If key is missing but required by @Idempotent(required = true)
        if ((key == null || key.isBlank()) && idempotentAnnotation != null && idempotentAnnotation.required()) {
            sendProblemDetail(request, response, HttpStatus.BAD_REQUEST, "urn:mtms:error:missing-idempotency-key",
                    "Missing Idempotency Key", "Header 'Idempotency-Key' is required for this endpoint.");
            return;
        }

        // 2. If no idempotency key provided, proceed normally
        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Validate key length (max 160 characters per DB schema)
        if (key.length() > 160) {
            sendProblemDetail(request, response, HttpStatus.BAD_REQUEST, "urn:mtms:error:invalid-idempotency-key",
                    "Invalid Idempotency Key", "Idempotency-Key header length cannot exceed 160 characters.");
            return;
        }

        // 4. Resolve current authenticated actor user ID
        UUID actorUserId = currentActorProvider.getCurrentActorId();
        if (actorUserId == null) {
            // Not authenticated: let filter chain continue so Spring Security produces 401/403
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Wrap request to cache and hash body
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestHash = computeSha256(cachedRequest.getCachedBody());

        long ttlSeconds = (idempotentAnnotation != null) ? idempotentAnnotation.ttlSeconds() : 86400L;

        // 6. Acquire key lock or retrieve cached response
        AcquireResult acquireResult;
        try {
            acquireResult = idempotencyService.tryAcquire(
                    actorUserId,
                    key,
                    requestHash,
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (IdempotencyConflictException ex) {
            sendProblemDetail(request, response, HttpStatus.CONFLICT, "urn:mtms:error:idempotency-in-progress",
                    "Idempotency In Progress", ex.getMessage());
            return;
        } catch (IdempotencyPayloadMismatchException ex) {
            sendProblemDetail(request, response, HttpStatus.UNPROCESSABLE_ENTITY, "urn:mtms:error:idempotency-payload-mismatch",
                    "Idempotency Payload Mismatch", ex.getMessage());
            return;
        } catch (Exception ex) {
            log.error("Unexpected error acquiring idempotency key '{}'", key, ex);
            if (exceptionResolver != null && exceptionResolver.resolveException(request, response, null, ex) != null) {
                return;
            }
            sendProblemDetail(request, response, HttpStatus.INTERNAL_SERVER_ERROR, "urn:mtms:error:internal-error",
                    "Internal Server Error", ex.getMessage());
            return;
        }

        // 7. If cached: return cached replay immediately
        if (acquireResult.isCached()) {
            IdempotencyRecord cached = acquireResult.record();
            response.setStatus(cached.getResponseStatus() != null ? cached.getResponseStatus() : HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(IDEMPOTENT_REPLAYED_HEADER, "true");
            if (cached.getResponseBody() != null) {
                response.getWriter().write(cached.getResponseBody());
            }
            return;
        }

        // 8. If newly acquired: execute endpoint, capture response and record outcome
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);
            int statusCode = responseWrapper.getStatus();
            byte[] respBytes = responseWrapper.getContentAsByteArray();
            String respBody = new String(respBytes, StandardCharsets.UTF_8);

            if (statusCode >= 200 && statusCode < 300) {
                idempotencyService.recordSuccess(actorUserId, key, statusCode, respBody);
            } else {
                idempotencyService.recordFailure(actorUserId, key, statusCode, respBody);
            }
        } catch (Exception ex) {
            idempotencyService.recordFailure(actorUserId, key, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            throw ex;
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private String extractIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader("idempotency-key");
        }
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    private Idempotent findIdempotentAnnotation(HttpServletRequest request) {
        if (handlerMapping == null) {
            return null;
        }
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                Idempotent methodAnn = handlerMethod.getMethodAnnotation(Idempotent.class);
                if (methodAnn != null) {
                    return methodAnn;
                }
                return handlerMethod.getBeanType().getAnnotation(Idempotent.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data != null ? data : new byte[0]);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void sendProblemDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String typeUri,
            String title,
            String detail
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(typeUri));
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
