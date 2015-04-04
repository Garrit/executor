package org.garrit.executor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

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
    private static final int COMPILE_TIMEOUT = 60;

    public JavaExecutor(RegisteredSubmission submission, ExecutionEnvironment environment) throws IOException
    {
        super(submission, environment);
    }

    @Override
    public void compile() throws IOException
    {
        ArrayList<String> command = new ArrayList<>();
        command.add("javac");

        for (SubmissionFile file : this.getSubmission().getFiles())
        {
            command.add(this.getUnpackedPath().resolve(file.getFilename()).toString());
        }

        EnvironmentResponse response = this.getEnvironment().execute(command, COMPILE_TIMEOUT);
        if (response.exitCode != EnvironmentResponse.SUCCESS)
            throw new IOException(
                    String.format("Received non-0 exit code (%d): \n%s",
                            response.exitCode, response.stderr));
    }

    @Override
    public ExecutionCase evaluate(ProblemCase problemCase) throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName(problemCase.getName());

        EnvironmentResponse response;

        ArrayList<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("/usr/local/bin/cputime", "java", "-cp"));
        command.add(this.getUnpackedPath().toString());
        command.add(this.getSubmission().getEntryPoint());

        String input = (problemCase.getInput() != null) ? new String(problemCase.getInput()) : null;

        try
        {
            response = this.getEnvironment().execute(
                    command,
                    input,
                    problemCase.getTimeLimit());
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