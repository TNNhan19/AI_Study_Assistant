package com.example.aistudyassistant.api;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);

    default void onWaitingForNetwork() {
        // Activity chỉ override khi cần hiển thị trạng thái chờ mạng.
    }
}
