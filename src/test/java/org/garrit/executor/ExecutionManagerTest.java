package org.garrit.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Paths;

import org.garrit.common.messages.RegisteredSubmission;
import org.junit.Test;

public class ExecutionManagerTest
{
    @Test
    public void testEnqueuesProblems() throws Exception
    {
        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setId(0);
        submission.setLanguage("foo");

        ExecutionManager executor = new ExecutionManager(Paths.get("."), new URI(""));
        executor.enqueue(submission);
        executor.close();

        assertEquals(1, executor.getQueued().size());
        assertTrue(executor.getQueued().contains(0));
    }
}