package com.example.myapplication.network;

/**
 * Generic callback interface for API operations
 * @param <T> The type of result expected from the API call
 */
public interface ApiCallback<T> {
    /**
     * Called when the API operation succeeds
     * @param result The result data
     */
    void onSuccess(T result);
    
    /**
     * Called when the API operation fails
     * @param error The error message
     */
    default void onError(String error) {
        // Default error handling
    }
}
