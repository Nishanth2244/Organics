// ShiprocketException.java
package com.organics.products.exception;

public class ShiprocketException extends RuntimeException {
    public ShiprocketException(String message) {
        super(message);
    }
    
    public ShiprocketException(String message, Throwable cause) {
        super(message, cause);
    }
}