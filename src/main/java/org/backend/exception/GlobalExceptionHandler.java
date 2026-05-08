package org.backend.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.backend.dto.common.ErrorResponseDTO;
import org.backend.dto.common.FieldValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,  HttpServletRequest request) {
        log.warn("Method not allowed | {} | UnsupportedMethod={}", requestInfo(request), ex.getMethod());
        String message = "Method " + ex.getMethod() + " is not supported for this endpoint.";
        return new ResponseEntity<>(build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", message), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,  HttpServletRequest request) {
        log.warn("Unsupported media type | {}", requestInfo(request));
        return new ResponseEntity<>(build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported Media Type"), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,  HttpServletRequest request) {
        log.warn(
                "Invalid parameter type | {} | Param={} | Value={}",
                requestInfo(request),
                ex.getName(),
                ex.getValue()
        );
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected a number.", ex.getValue(), ex.getName());
        return new ResponseEntity<>(
                build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message), HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
//        List<FieldValidationError> fieldErrors = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(err -> new FieldValidationError(err.getField(), err.getDefaultMessage()))
//                .toList();

        log.warn(
                "Validation failed | {} | ErrorCount={}",
                requestInfo(request),
                ex.getBindingResult().getErrorCount()
        );
        log.debug(
                "Validation details | {}",
                ex.getBindingResult().getFieldErrors()
        );
        Map<String, String> errorMap = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            errorMap.putIfAbsent(err.getField(), err.getDefaultMessage());
        });
        List<FieldValidationError> fieldErrors = errorMap.entrySet()
                .stream()
                .map(e -> new FieldValidationError(e.getKey(), e.getValue()))
                .toList();
        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .status_code(HttpStatus.BAD_REQUEST.value())
                .status("VALIDATION_ERROR")
                .message("Input validation failed")
                .fieldErrors(fieldErrors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn(
                "Duplicate resource | {} | Message={}",
                requestInfo(request),
                ex.getMessage()
        );
        return new ResponseEntity<>(
                build(HttpStatus.BAD_REQUEST, "DUPLICATE_RESOURCE", ex.getMessage()), HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn(
                "Resource not found | {} | Message={}",
                requestInfo(request),
                ex.getMessage()
        );
        return new ResponseEntity<>(
                build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage()), HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponseDTO> handleOtp(OtpException ex, HttpServletRequest request) {
        log.warn(
                "OTP validation failed | {} | Code={}",
                requestInfo(request),
                ex.getCode()
        );
        return new ResponseEntity<>(
                build(ex.getStatusCode(), ex.getCode(), ex.getMessage()), ex.getStatusCode()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn(
                "Authentication failed | {}",
                requestInfo(request)
        );
        log.debug("Authentication exception", ex);
        return new ResponseEntity<>(
                build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid credentials"), HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponseDTO> handleJwt(JwtException ex, HttpServletRequest request) {
        log.warn(
                "JWT validation failed | {}",
                requestInfo(request)
        );
        log.debug("JWT error details", ex);

        return new ResponseEntity<>(
                build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "JWT token is invalid or expired"), HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn(
                "Access denied | {}",
                requestInfo(request)
        );
        return new ResponseEntity<>(
                build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You don't have permission to access this resource"), HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials | {}", requestInfo(request));
        return new ResponseEntity<>(
                build(HttpStatus.BAD_REQUEST, "BAD_CREDENTIALS", "Invalid mobile number or password"), HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request | {} | Message={}", requestInfo(request), ex.getMessage());
        return new ResponseEntity<>(
                build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage()), HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDTO> noHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found | {}", requestInfo(request));
        return new ResponseEntity<>(
                build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage()), HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid JSON payload | {}", requestInfo(request));
        log.debug("JSON parsing exception", ex);

        String message = "Invalid request payload";
        String status = "INVALID_JSON_DATA";

        String errorMsg = ex.getMessage();
        String rootMsg = ex.getMostSpecificCause().getMessage();
        if (errorMsg.contains("Unrecognized token") || rootMsg.contains("Unrecognized token")) {
            message = "Invalid JSON format. Check for missing quotes or incorrect values.";
            status = "MALFORMED_JSON";
        } else if (errorMsg.contains("Cannot construct instance") || rootMsg.contains("Cannot construct instance")) {
            // Check if it's really ENUM
            if (errorMsg.contains("enum") || rootMsg.contains("enum")) {
                message = "Invalid value provided. Please check enum values.";
                status = "INVALID_ENUM";
            }
            // Otherwise it's STRUCTURE issue
            else {
                message = "Invalid JSON structure. Please close the brackets properly.";
                status = "INVALID_JSON_STRUCTURE";
            }
        } else if (errorMsg.contains("Unexpected end-of-input") || rootMsg.contains("Unexpected end-of-input")) {
            message = "Invalid JSON structure. Please close the brackets properly.";
            status = "INVALID_JSON_STRUCTURE";
        } else if (errorMsg.contains("Unexpected character") || rootMsg.contains("Unexpected character")) {
            message = "Invalid JSON syntax. Check commas and structure.";
            status = "INVALID_JSON_SYNTAX";
        }
        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .status_code(HttpStatus.BAD_REQUEST.value())
                .status(status)
                .message(message)
                .error(ex.getMessage())
                .build();

        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobal(Exception ex, HttpServletRequest request) {
        log.error("Unexpected server error | {}", requestInfo(request), ex);
        return new ResponseEntity<>(
                build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Something went wrong. Please try again later."), HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ErrorResponseDTO build(HttpStatus status, String type, String message) {
        return ErrorResponseDTO.builder()
                .status_code(status.value())
                .status(type)
                .message(message)
                .build();
    }

    private String requestInfo(HttpServletRequest request) {
        return String.format(
                "URI=%s | Method=%s | TraceId=%s",
                request.getRequestURI(),
                request.getMethod(),
                MDC.get("traceId")
        );
    }
}
