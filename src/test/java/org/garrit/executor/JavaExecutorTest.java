package org.garrit.executor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.SubmissionFile;
import org.junit.Test;

/**
 * Test the {@link JavaExecutor Java executor}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class JavaExecutorTest
{
    @Test
    public void testCompilationCommand() throws IOException
    {
        SubmissionFile hello = new SubmissionFile();
        hello.setFilename("Hello.java");

        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setFiles(Arrays.asList(hello));

        SpyEnvironment environment = new SpyEnvironment();

        try (JavaExecutor executor = new JavaExecutor(submission, environment))
        {
            executor.compile();
        }

        assertEquals("javac /input/Hello.java", environment.getCommand());
    }

    @Test
    public void testEvaluationCommand() throws IOException
    {
        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setEntryPoint("Hello");

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("Test case");
        problemCase.setTimeLimit(0);

        SpyEnvironment environment = new SpyEnvironment();

        try (JavaExecutor executor = new JavaExecutor(submission, environment))
        {
            executor.evaluate(problemCase);
        }

        assertEquals("cputime java -cp /input Hello", environment.getCommand());
    }

    @Getter
    public static class SpyEnvironment extends ExecutionEnvironment
    {
        public static final String UNPACK_PATH = "/input/";
        public static final String INPUT_FILENAME = UNPACK_PATH + "input-00";

        private List<SubmissionFile> files;
        private byte[] input;
        private List<String> command;
        private String commandInput;
        private boolean closed = false;

        @Override
        public Path unpack(List<SubmissionFile> files)
        {
            this.files = files;
            return Paths.get(UNPACK_PATH);
        }

        @Override
        public Path unpackInput(byte[] input)
        {
            this.input = input;
            return Paths.get(INPUT_FILENAME);
        }

        @Override
        public EnvironmentResponse execute(List<String> command, String input, long timeout) throws IOException
        {
            this.command = command;
            this.commandInput = input;
            return new EnvironmentResponse(0, "", "0");
        }

        @Override
        public void close() throws IOException
        {
            this.closed = true;
        }
    }
}