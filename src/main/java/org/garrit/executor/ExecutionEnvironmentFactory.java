package org.garrit.executor;

import java.io.IOException;

/**
 * Provide acess to {@link ExecutionEnvironment execution environments}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class ExecutionEnvironmentFactory
{
    /**
     * @return a new execution environment
     */
    public static ExecutionEnvironment geExecutionEnvironment() throws IOException
    {
        return new LXCEnvironment();
    }
}