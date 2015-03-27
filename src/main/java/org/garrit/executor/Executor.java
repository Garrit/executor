package org.garrit.executor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import lombok.Getter;

import org.garrit.common.Problem;
import org.garrit.common.ProblemCase;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.Submission;

/**
 * An executor is capable of instructing the {@link ExecutionEnvironment
 * environment} to evaluate a {@link Submission submission}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public abstract class Executor implements Closeable
{
    @Getter
    private final RegisteredSubmission submission;
    @Getter
    private final ExecutionEnvironment environment;
    @Getter
    private final Path unpackedPath;

    /**
     * Set up the executor for a given submission and environment. The default
     * constructor unpacks the submission files into the environment.
     * 
     * @param submission the submission
     * @param environment environment
     */
    public Executor(RegisteredSubmission submission, ExecutionEnvironment environment) throws IOException
    {
        this.submission = submission;
        this.environment = environment;

        this.unpackedPath = this.environment.unpack(submission.getFiles());
    }

    /**
     * Perform any necessary compilation of the submission. To be called before
     * any invocations of <code>{@link Executor#evaluate(Problem)}</code>. This
     * may be a no-op for executors for interpreted languages.
     */
    public abstract void compile();

    /**
     * Execute the submission for a given problem case.
     * 
     * @param problemCase the problem case
     * @return the results of problem execution
     * @throws IOException if an error occurs while evaluating
     */
    public abstract ExecutionCase evaluate(ProblemCase problemCase) throws IOException;

    /**
     * Clean up the environment.
     */
    @Override
    public void close() throws IOException
    {
        this.environment.close();
    }
}