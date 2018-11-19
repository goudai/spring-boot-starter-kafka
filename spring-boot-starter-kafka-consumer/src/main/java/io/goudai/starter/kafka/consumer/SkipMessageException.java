package io.goudai.starter.kafka.consumer;

public class SkipMessageException extends RuntimeException {

    public SkipMessageException() {
        super();
    }

    public SkipMessageException(String message) {
        super(message);
    }

    public SkipMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkipMessageException(Throwable cause) {
        super(cause);
    }

    protected SkipMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
