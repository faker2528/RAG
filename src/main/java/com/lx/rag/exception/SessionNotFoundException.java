package com.lx.rag.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public SessionNotFoundException(String message) {
        super(message);
    }
}
