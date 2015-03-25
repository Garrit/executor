package org.garrit.executor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

import org.garrit.common.Problems;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.statuses.ExecutorStatus;

/**
 * Handle execution of submissions.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Slf4j
public class ExecutionManager implements ExecutorStatus
{
    private Path problems;

    LinkedBlockingQueue<RegisteredSubmission> queue = new LinkedBlockingQueue<>();

    public ExecutionManager(Path problems)
    {
        this.problems = problems;
    }

    /**
     * Enqueue a submission for execution.
     * 
     * @param submission the submission
     */
    public void enqueue(RegisteredSubmission submission)
    {
        this.queue.add(submission);
    }

    @Override
    public Iterable<String> getLanguages()
    {
        return null;
    }

    @Override
    public Iterable<String> getProblems()
    {
        try
        {
            return Problems.availableProblems(this.problems);
        }
        catch (IOException e)
        {
            log.warn("Failure evaluating available problems", e);
            return Arrays.asList();
        }
    }

    @Override
    public ArrayList<Integer> getQueued()
    {
        ArrayList<RegisteredSubmission> frozenQueue = new ArrayList<>(this.queue);
        ArrayList<Integer> queuedIds = new ArrayList<>(frozenQueue.size());

        frozenQueue.forEach(submission -> queuedIds.add(submission.getId()));

        return queuedIds;
    }
}