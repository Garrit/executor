package org.garrit.executor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
     * Unpack a collection of submission files into the environment. An
     * implementation is responsible for destroying any files created within the
     * environment by this method when <code>{@link #close()}</code> is called.
     * 
     * @param files the files to unpack
     * @return the path of the parent directory to the unpacked files
     * @throws IOException if an error occurs while unpacking
     */
    public abstract Path unpack(List<SubmissionFile> files) throws IOException;

    /**
     * Create a file within the environment for input to a program. The
     * environment is responsible for generating a filename for the file and
     * locating it within the environment such that it does not conflict with
     * any other files.
     * 
     * As with files {@link #unpack(SubmissionFile[] unpacked} from the
     * submission, an implementation is responsible for destroying any files
     * created by this method when <code>{@link #close()}</code> is called.
     * 
     * @param input the input file contents
     * @return the path of the file within the environment
     * @throws IOException if an error occurs while unpacking
     */
    public abstract Path unpackInput(byte[] input) throws IOException;

    /**
     * Execute a command within the environment.
     * 
     * @param command the command to execute
     * @param timeout the timeout for execution in seconds
     * @return the output of the command
     * @throws IOException if a failure occurs while executing the command
     */
    public abstract EnvironmentResponse execute(String command, long timeout) throws IOException;

    /**
     * The response to executing a command in the environment.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    @RequiredArgsConstructor
    public static class EnvironmentResponse
    {
        /**
         * The exit code of the process.
         */
        public final int exitCode;
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