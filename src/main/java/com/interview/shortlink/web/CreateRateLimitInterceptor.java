package com.interview.shortlink.web;

import com.interview.shortlink.exception.TooManyRequestsException;
import com.interview.shortlink.service.CreateRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CreateRateLimitInterceptor implements HandlerInterceptor {

    private final CreateRateLimiter limiter;

    public CreateRateLimitInterceptor(CreateRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        if (!limiter.allow(request.getRemoteAddr())) {
            throw new TooManyRequestsException("Too many short-link creation requests; retry later");
        }
        return true;
    }
}
