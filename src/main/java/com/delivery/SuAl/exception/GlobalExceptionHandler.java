package com.delivery.SuAl.exception;

import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("No resource found: {} at {}", ex.getMessage(), request.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, "Resource not found", request);
    }


    @ExceptionHandler({
            BusinessRuleViolationException.class,
            InsufficientStockException.class,
            InsufficientContainerException.class,
            InvalidRequestException.class,
            NotValidException.class,
            TransferSameWarehouseException.class,
            ActivePriceAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: {} at {}", ex.getMessage(), request.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed for request {}: {}", request.getRequestURI(), details);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed: " + details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation for request {}: {}", request.getRequestURI(), details);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed: " + details, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Type mismatch: {} at {}", message, request.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed: {} at {}", ex.getMessage(), request.getRequestURI());
        return buildError(HttpStatus.UNAUTHORIZED, "Authentication failed", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad credentials at {}", request.getRequestURI());
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
    }


    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOperation(
            UnauthorizedOperationException ex,
            HttpServletRequest request
    ) {
        log.warn("Unauthorized operation: {} at {} by user from IP: {}",
                ex.getMessage(), request.getRequestURI(), request.getRemoteAddr());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied at {} for user from IP: {}",
                request.getRequestURI(), request.getRemoteAddr());
        return buildError(HttpStatus.FORBIDDEN, "Access denied", request);
    }


    @ExceptionHandler({
            AlreadyExistsException.class,
            InvalidOrderStateException.class,
            PromoUsageLimitExceededException.class,
            CampaignUsageLimitExceededException.class,
            AlreadyPaidException.class,
            InvalidPaymentStateException.class,
            OrderDeletionException.class,
            InvoiceNotEditableException.class,
            InvoiceAlreadyApprovedException.class,
            TransferNotEditableException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict: {} at {}", ex.getMessage(), request.getRequestURI());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }


    @ExceptionHandler({
            PaymentCreationException.class,
            PaymentRefundException.class,
            PaymentVerificationException.class
    })
    public ResponseEntity<ErrorResponse> handlePaymentProcessingException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Payment processing error [{}]: {} at {}",
                errorId, ex.getMessage(), request.getRequestURI(), ex);

        String userMessage = isProduction()
                ? "Payment processing failed. Please try again or contact support. Reference ID: " + errorId
                : ex.getMessage();

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, userMessage, request, errorId);
    }

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(
            GatewayException ex,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Payment gateway error [{}]: {} at {}",
                errorId, ex.getMessage(), request.getRequestURI(), ex);

        String userMessage = isProduction()
                ? "Payment gateway temporarily unavailable. Please try again later. Reference ID: " + errorId
                : ex.getMessage();

        return buildError(HttpStatus.BAD_GATEWAY, userMessage, request, errorId);
    }


    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ErrorResponse> handleImageUploadException(
            ImageUploadException ex,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Image upload failed [{}]: {} at {}",
                errorId, ex.getMessage(), request.getRequestURI(), ex);

        String userMessage = isProduction()
                ? "Image upload failed. Please try again. Reference ID: " + errorId
                : ex.getMessage();

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, userMessage, request, errorId);
    }


    @ExceptionHandler(TransientPushException.class)
    public ResponseEntity<ErrorResponse> handleTransientPushException(
            TransientPushException ex,
            HttpServletRequest request
    ) {
        log.warn("Push notification failed (non-critical): {} at {}",
                ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .error("Warning")
                .message("Operation completed but notification delivery failed")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex) {
        String supported = ex.getSupportedMediaTypes().stream()
                .map(MediaType::toString)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(
                        "Unsupported Content-Type '" + ex.getContentType() +
                                "'. Supported: [" + supported + "]"));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error [{}]: {} at {}",
                errorId, ex.getMessage(), request.getRequestURI(), ex);

        String userMessage = isProduction()
                ? "An unexpected error occurred. Please contact support with reference ID: " + errorId
                : ex.getMessage();

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, userMessage, request, errorId);
    }


    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            String errorId
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(sanitizeMessage(message))
                .path(request.getRequestURI())
                .errorId(errorId)
                .build();

        return ResponseEntity.status(status).body(response);
    }


    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(sanitizeMessage(message))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }


    private String sanitizeMessage(String message) {
        if (message == null) {
            return "An error occurred";
        }

        if (isProduction()) {
            if (message.toLowerCase().contains("sql") ||
                    message.toLowerCase().contains("database") ||
                    message.toLowerCase().contains("constraint")) {
                return "A database error occurred. Please contact support.";
            }

            if (message.contains("java.") || message.contains("org.")) {
                return "An internal error occurred. Please contact support.";
            }
        }

        return message;
    }

    private boolean isProduction() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}