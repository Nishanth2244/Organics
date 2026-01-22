package com.organics.products.exception;

public class PincodeNotFoundException extends RuntimeException {
    public PincodeNotFoundException(String message) {
        super(message);
    }
}
