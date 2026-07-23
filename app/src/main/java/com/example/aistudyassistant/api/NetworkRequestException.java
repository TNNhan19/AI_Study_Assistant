package com.example.aistudyassistant.api;

/** Đánh dấu lỗi truyền tải để service có thể chờ mạng và retry. */
public class NetworkRequestException extends RuntimeException {
    public NetworkRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
