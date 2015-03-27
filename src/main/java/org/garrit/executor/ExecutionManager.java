package org.garrit.executor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

import org.garrit.common.Problem;
import org.garrit.common.ProblemCase;
import org.garrit.common.Problems;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.statuses.ExecutorStatus;

/**
 * Handle execution of submissions.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Slf4j
public class ExecutionManager implements ExecutorStatus, Closeable
{
    /**
     * The path containing problem definitions.
     */
    private final Path problems;
    private final ExecutionThread executionThread;
    private final ReportThread reportThread;

    /**
     * Submissions lined up and waiting to be executed.
     */
    LinkedBlockingQueue<RegisteredSubmission> submissionQueue = new LinkedBlockingQueue<>();
    /**
     * Submissions which have been executed and need to be sent back to the
     * mediator.
     */
    LinkedBlockingQueue<Execution> outgoingQueue = new LinkedBlockingQueue<>();

    public ExecutionManager(Path problems)
    {
        this.problems = problems;
        this.executionThread = new ExecutionThread();
        this.reportThread = new ReportThread();
    }

    /**
     * Enqueue a submission for execution.
     * 
     * @param submission the submission
     */
    public void enqueue(RegisteredSubmission submission) throws UnavailableExecutorException
    {
        if (ExecutorFactory.executorExists(submission))
            throw new UnavailableExecutorException("No executor available for language");

        this.submissionQueue.add(submission);
    }

    @Override
    public Iterable<String> getLanguages()
    {
        return ExecutorFactory.availableLanguages();
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
        ArrayList<RegisteredSubmission> frozenQueue = new ArrayList<>(this.submissionQueue);
        ArrayList<Integer> queuedIds = new ArrayList<>(frozenQueue.size());

        frozenQueue.forEach(submission -> queuedIds.add(submission.getId()));

        return queuedIds;
    }

    /**
     * Start processing queued submissions.
     */
    public void start()
    {
        log.info("Starting execution manager");
        this.executionThread.start();
        this.reportThread.start();
    }

    @Override
    public void close() throws IOException
    {
        log.info("Closing execution manager");
        this.executionThread.interrupt();
        this.reportThread.interrupt();
    }

    /**
     * Thread to perform the actual executions.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ExecutionThread extends Thread
    {
        @Override
        public void run()
        {
            log.info("Starting execution thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    RegisteredSubmission submission = ExecutionManager.this.submissionQueue.take();

                    Problem problem;
                    ExecutionEnvironment environment;
                    Executor executor;

                    try
                    {
                        problem = Problems.problemByName(problems, submission.getProblem());
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to retrieve problem definition", e);
                        continue;
                    }

                    try
                    {
                        environment = ExecutionEnvironmentFactory.getExecutionEnvironment();
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to retrieve an execution environment", e);
                        continue;
                    }

                    try
                    {
                        executor = ExecutorFactory.getExecutor(submission, environment);
                    }
                    catch (UnavailableExecutorException e)
                    {
                        log.error("No executor available for submission", e);
                        continue;
                    }

                    ArrayList<ExecutionCase> executionCases = new ArrayList<>();
                    for (ProblemCase problemCase : problem.getCases())
                    {
                        try
                        {
                            executionCases.add(executor.evaluate(problemCase));
                        }
                        catch (IOException e)
                        {
                            log.error("Failure while evaluating case", e);
                        }
                    }

                    Execution execution = new Execution(submission);
                    execution.setCases(executionCases);

                    ExecutionManager.this.outgoingQueue.offer(execution);
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing execution thread");
        }
    }

    /**
     * Thread to report back to the mediator.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ReportThread extends Thread
    {
        @Override
        public void run()
        {
            log.info("Starting mediator reporting thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    Execution execution = ExecutionManager.this.outgoingQueue.take();
                    log.info("Asked to push {} out to the mediator", execution);
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing mediator reporting thread");
        }
    }
}