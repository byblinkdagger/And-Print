package com.oragee.kneemeasure.blt;

public class BlueException extends RuntimeException {
    public BlueException() {
        super();
    }

    public BlueException(String message) {
        super(message);
    }

    public BlueException(Throwable cause) {
        super(cause);
    }

    public BlueException(String message, Throwable cause) {
        super(message, cause);
    }
}
