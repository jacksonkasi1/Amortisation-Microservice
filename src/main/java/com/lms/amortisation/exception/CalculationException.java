package com.lms.amortisation.exception;

/**
 * Exception thrown when amortisation calculation fails
 *
 * @author LMS Team
 * @version 1.0.0
 */
public class CalculationException extends RuntimeException {

    private String errorCode;

    public CalculationException(String message) {
        super(message);
    }

    public CalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CalculationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CalculationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
