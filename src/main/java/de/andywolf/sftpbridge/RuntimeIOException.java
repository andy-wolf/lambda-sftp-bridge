package de.andywolf.sftpbridge;

/**
 * Thrown whenever an I/O error occurs.
 */
@SuppressWarnings("serial")
public class RuntimeIOException extends RuntimeException {

    /**
     * Constructs an <code>RuntimeIOException</code> with <code>null</code> as its detail message.
     */
    public RuntimeIOException() {
        super();
    }

    /**
     * Constructs an <code>RuntimeIOException</code> with the specified detail message.
     *
     * @param message the detail message.
     */
    public RuntimeIOException(String message) {
        super(message);
    }

    /**
     * Constructs an <code>RuntimeIOException</code> with specified cause and a detail message of (cause==null ? null :
     * cause.toString()) (which typically contains the class and detail message of cause).
     *
     * @param cause the root cause
     */
    public RuntimeIOException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an <code>RuntimeIOException</code> with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the root cause
     */
    public RuntimeIOException(String message, Throwable cause) {
        super(message, cause);
    }

}
