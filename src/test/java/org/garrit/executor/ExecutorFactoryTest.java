package org.garrit.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.SubmissionFile;
import org.junit.Test;

/**
 * Test the {@link ExecutorFactory executor factory}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class ExecutorFactoryTest
{
    @Test
    public void testAvailableLanguages()
    {
        ExecutorFactory.registerExecutor("test", MockExecutor.class);
        assertTrue(ExecutorFactory.availableLanguages().contains("test"));
    }

    @Test
    public void testExecutorInstantiation() throws UnavailableExecutorException
    {
        ExecutorFactory.registerExecutor("test", MockExecutor.class);

        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setLanguage("test");

        Executor executor = ExecutorFactory.getExecutor(submission, new MockEnvironment());
        assertEquals(MockExecutor.class, executor.getClass());
    }

    @Test(expected = UnavailableExecutorException.class)
    public void testFailingExecutorRetrieval() throws UnavailableExecutorException
    {
        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setLanguage("nope");
        ExecutorFactory.getExecutor(submission, new MockEnvironment());
    }

    public static class MockEnvironment extends ExecutionEnvironment
    {
        @Override
        public Path unpack(List<SubmissionFile> files)
        {
            return Paths.get(".");
        }

        @Override
        public Path unpackInput(byte[] input)
        {
            return null;
        }

        @Override
        public EnvironmentResponse execute(String command, long timeout) throws IOException
        {
            return null;
        }

        @Override
        public void close() throws IOException
        {
        }
    }

    public static class MockExecutor extends Executor
    {
        public MockExecutor(RegisteredSubmission submission, ExecutionEnvironment environment)
        {
            super(submission, environment);
        }

        @Override
        public void compile()
        {
        }

        @Override
        public ExecutionCase evaluate(ProblemCase problemCase)
        {
            return null;
        }
    }
}