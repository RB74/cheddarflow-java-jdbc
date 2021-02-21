package com.cheddarflow.jdbc;

public class NoDataInRangeException extends RuntimeException {

    public NoDataInRangeException() {
    }

    public NoDataInRangeException(String message) {
        super(message);
    }

    public NoDataInRangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDataInRangeException(Throwable cause) {
        super(cause);
    }

    public NoDataInRangeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
