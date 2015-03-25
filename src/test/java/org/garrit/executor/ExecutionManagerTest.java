package org.garrit.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;

import org.garrit.common.messages.RegisteredSubmission;
import org.junit.Test;

public class ExecutionManagerTest
{
    @Test
    public void testEnqueuesProblems()
    {
        RegisteredSubmission submission = new RegisteredSubmission();
        submission.setId(0);

        ExecutionManager executor = new ExecutionManager(Paths.get("."));
        executor.enqueue(submission);

        assertEquals(1, executor.getQueued().size());
        assertTrue(executor.getQueued().contains(0));
    }
}