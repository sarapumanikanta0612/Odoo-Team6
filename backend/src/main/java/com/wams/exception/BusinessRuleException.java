package com.wams.exception;

/**
 * Thrown when a domain invariant (e.g. the WeddingProject creation gate)
 * is violated by a client request.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
