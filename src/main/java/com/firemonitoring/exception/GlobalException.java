package com.firemonitoring.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            BadRequestException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
        String message = exception instanceof BadRequestException
                ? exception.getMessage()
                : "Invalid request";
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({AuthenticationException.class, UnauthorizedException.class})
    public ResponseEntity<ProblemDetail> handleUnauthorized(
            Exception exception,
            HttpServletRequest request) {
        String message = exception instanceof UnauthorizedException
                ? exception.getMessage()
                : "Authentication is required";
        return response(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ProblemDetail> handleForbidden(
            Exception exception,
            HttpServletRequest request) {
        String message = exception instanceof ForbiddenException
                ? exception.getMessage()
                : "You do not have permission to access this resource";
        return response(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler({
            EntityNotFoundException.class,
            NoSuchElementException.class,
            NoHandlerFoundException.class,
            NoResourceFoundException.class,
            FindNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(Exception exception, HttpServletRequest request) {
        String message = exception instanceof FindNotFoundException
                ? exception.getMessage()
                : "Resource not found";
        return response(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported", request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConflictException.class})
    public ResponseEntity<ProblemDetail> handleConflict(
            Exception exception,
            HttpServletRequest request) {
        String message = exception instanceof ConflictException
                ? exception.getMessage()
                : "The request conflicts with existing data";
        return response(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return response(status, message, request, Map.of());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        if (!validationErrors.isEmpty()) {
            problem.setProperty("validationErrors", validationErrors);
        }
        return ResponseEntity.status(status).body(problem);
    }
}
