package com.interview.shortlink.web;

import com.interview.shortlink.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(LinkUnavailableException.class)
    ProblemDetail unavailable(LinkUnavailableException ex, HttpServletRequest request) {
        return problem(HttpStatus.GONE, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class})
    ProblemDetail badRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ProblemDetail rateLimited(TooManyRequestsException ex, HttpServletRequest request) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, detail, request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
