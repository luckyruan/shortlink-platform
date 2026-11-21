package com.interview.shortlink.exception;

public class LinkUnavailableException extends RuntimeException {
    public LinkUnavailableException(String message) { super(message); }
}
