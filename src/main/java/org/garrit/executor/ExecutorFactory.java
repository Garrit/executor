package org.garrit.executor;

import org.garrit.common.messages.RegisteredSubmission;

/**
 * Provides access to {@link Executor problem executors}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class ExecutorFactory
{
    /**
     * Get an executor suitable for the given submission and configured for a
     * target execution environment.
     * 
     * @param submission the submission
     * @param environment the execution environment
     * @return a suitable executor
     * @throws UnavailableExecutorException if there is no matching executor
     */
    public static Executor getExecutor(RegisteredSubmission submission, ExecutionEnvironment environment)
            throws UnavailableExecutorException
    {
        throw new UnavailableExecutorException(
                String.format(
                        "No executor available for \"%s\" submissions",
                        submission.getLanguage()));
    }
}