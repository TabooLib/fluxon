package org.tabooproject.fluxon.runtime.stdlib;

public class IntrinsicException extends RuntimeException {

    public IntrinsicException(String message) {
        super(message);
    }

    public IntrinsicException(String message, Throwable cause) {
        super(message, cause);
    }
}
