package id.ac.ui.cs.advprog.papikos.notification.exception;

public class ServiceUnavailableException extends Exception { // Changed from Throwable to Exception

    /**
     * Constructs a new ServiceUnavailableException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ServiceUnavailableException(String message) {
        super(message); // Call superclass constructor
    }

    /**
     * Constructs a new ServiceUnavailableException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause); // Call superclass constructor
    }
}