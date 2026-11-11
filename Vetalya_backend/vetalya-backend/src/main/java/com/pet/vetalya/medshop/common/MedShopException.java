package com.pet.vetalya.medshop.common;

import org.springframework.http.HttpStatus;

public class MedShopException extends RuntimeException {

    private final HttpStatus status;

    public MedShopException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static class NotFoundException extends MedShopException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    public static class ConflictException extends MedShopException {
        public ConflictException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    public static class BadRequestException extends MedShopException {
        public BadRequestException(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }

    public static class UnauthorizedException extends MedShopException {
        public UnauthorizedException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    public static class ForbiddenException extends MedShopException {
        public ForbiddenException(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
    }

    public static class UnprocessableException extends MedShopException {
        public UnprocessableException(String message) {
            super(message, HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }
}

