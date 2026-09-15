package com.dps.roboleague.application;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String type, String id) {
        return new NotFoundException(type + " " + id + " was not found");
    }
}
