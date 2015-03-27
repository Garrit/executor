package org.garrit.executor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.SubmissionFile;
import org.garrit.executor.ExecutionEnvironment.EnvironmentResponse;

/**
 * An {@link Executor executor} for Java submissions.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Slf4j
public class JavaExecutor extends Executor
{
    public JavaExecutor(RegisteredSubmission submission, ExecutionEnvironment environment) throws IOException
    {
        super(submission, environment);
    }

    /* TODO: Figure out how to best deal with execution exceptions at this
     * stage. Throw an exception and propagate it back to the mediator? */
    @Override
    public void compile()
    {
        StringBuilder command = new StringBuilder();
        command.append("javac");

        for (SubmissionFile file : this.getSubmission().getFiles())
        {
            command.append(" ");
            command.append(this.getUnpackedPath().resolve(file.getFilename()));
        }

        try
        {
            this.getEnvironment().execute(command.toString(), 0);
        }
        catch (IOException e)
        {
            log.error("Failed execution", e);
        }
    }

    @Override
    public ExecutionCase evaluate(ProblemCase problemCase) throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName(problemCase.getName());

        EnvironmentResponse response;

        Path input = this.getEnvironment().unpackInput(problemCase.getInput());
        String command = String.format(
                "cputime java -cp %s %s < %s",
                this.getUnpackedPath(), this.getSubmission().getEntryPoint(), input);

        try
        {
            response = this.getEnvironment().execute(command, problemCase.getTimeLimit());
        }
        catch (IOException e)
        {
            log.error(String.format("Failure while evaluating problem case \"%s\"", problemCase.getName()), e);

            executionCase.setErrorOccurred(true);
            executionCase.setError(e.getMessage());
            return executionCase;
        }

        /* cputime gives the runtime as the last line of stderr. */
        String[] stderrLines = response.stderr.split("\n");
        executionCase.setRuntime(Integer.valueOf(stderrLines[stderrLines.length - 1]));

        try
        {
            executionCase.setOutput(response.stdout.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            /* This should never happen: UTF-8 should be available as an
             * encoding on all OpenJDK/Oracle JVMs. */
            log.warn("Failed to encode environment output to UTF-8");
        }

        executionCase.setErrorOccurred(false);
        return executionCase;
    }
}