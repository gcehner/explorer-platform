package com.gregisoft.explorer.cli.http;

public class BackendClientException extends RuntimeException {

    public BackendClientException(String message) {
        super(message);
    }

    public BackendClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
