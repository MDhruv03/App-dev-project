package com.example.myapplication.network;

/**
 * API response wrapper with status and error handling
 */
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final int statusCode;
    
    private ApiResponse(boolean success, T data, String errorMessage, int statusCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, 200);
    }
    
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return new ApiResponse<>(false, null, message, statusCode);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public T getData() {
        return data;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
