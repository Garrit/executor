package org.garrit.executor;

import java.io.Closeable;

import lombok.RequiredArgsConstructor;

import org.garrit.common.messages.SubmissionFile;

/**
 * An environment in which submission can be executed.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public abstract class ExecutionEnvironment implements Closeable
{
    /**
     * Unpack a collection of submission files into the environment.
     * 
     * @param files the files to unpack
     */
    public abstract void unpack(SubmissionFile[] files);

    /**
     * Execute a command within the environment.
     * 
     * @param command the command to execute
     */
    public abstract EnvironmentResponse execute(String command);

    /**
     * The response to executing a command in the environment.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    @RequiredArgsConstructor
    public class EnvironmentResponse
    {
        /**
         * The response on stdout.
         */
        public final String stdout;
        /**
         * The response on stderr.
         */
        public final String stderr;
    }
}