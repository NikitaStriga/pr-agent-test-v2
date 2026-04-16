package ru.komus.idgenerator.data.exceptions;

/**
 * An <code>AlreadyExistsException</code> is thrown if an attempt
 * is made an object in the database with a name that already exists.
 */
public class AlreadyExistsException extends RuntimeException {

    /**
     * Constructs an <code>AlreadyExistsException</code> with no
     * specified detail message.
     */
    public AlreadyExistsException() {
        super();
    }

    /**
     * Constructs an <code>AlreadyExistsException</code> with the specified
     * detail message.
     */
    public AlreadyExistsException(String s) {
        super(s);
    }
}
