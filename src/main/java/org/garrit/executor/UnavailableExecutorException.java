package org.garrit.executor;

/**
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class UnavailableExecutorException extends Exception
{
    private static final long serialVersionUID = 1L;

    public UnavailableExecutorException(String message)
    {
        super(message);
    }

    public UnavailableExecutorException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}