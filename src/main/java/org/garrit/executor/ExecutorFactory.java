package org.garrit.executor;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.garrit.common.messages.RegisteredSubmission;

/**
 * Provides access to {@link Executor problem executors}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class ExecutorFactory
{
    private static HashMap<String, Class<? extends Executor>> executors = new HashMap<>();

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
        Class<? extends Executor> executorClass = executors.get(submission.getLanguage().toLowerCase());

        if (executorClass == null)
            throw new UnavailableExecutorException(
                    String.format(
                            "No executor available for \"%s\" submissions",
                            submission.getLanguage()));

        try
        {
            Constructor<? extends Executor> constructor;
            constructor = executorClass.getConstructor(RegisteredSubmission.class, ExecutionEnvironment.class);
            return constructor.newInstance(submission, environment);
        }
        catch (ReflectiveOperationException e)
        {
            throw new UnavailableExecutorException(
                    String.format(
                            "Failed to instantiate executor %s",
                            executorClass.getName()),
                    e);
        }
    }

    /**
     * Register an executor class for use.
     * 
     * @param language the language for which the executor should be used
     * @param executorClass the executor class
     */
    public static void registerExecutor(String language, Class<? extends Executor> executorClass)
    {
        executors.put(language.toLowerCase(), executorClass);
    }

    /**
     * @return languages for which executors exist
     */
    public static Set<String> availableLanguages()
    {
        return Collections.unmodifiableSet(executors.keySet());
    }
}