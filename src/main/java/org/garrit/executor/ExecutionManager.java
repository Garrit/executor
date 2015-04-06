package org.garrit.executor;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.garrit.common.Problem;
import org.garrit.common.ProblemCase;
import org.garrit.common.Problems;
import org.garrit.common.messages.ErrorSubmission;
import org.garrit.common.messages.ErrorType;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.statuses.CapabilityType;
import org.garrit.common.messages.statuses.ExecutorStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ErrorThread errorThread;

    /**
     * Submissions lined up and waiting to be executed.
     */
    LinkedBlockingQueue<RegisteredSubmission> submissionQueue = new LinkedBlockingQueue<>();
    /**
     * Submissions which have been executed and need to be sent back to the
     * negotiator.
     */
    LinkedBlockingQueue<Execution> outgoingQueue = new LinkedBlockingQueue<>();
    /**
     * Errors in execution which need to be indicated to the negotiator.
     */
    LinkedBlockingQueue<ErrorSubmission<RegisteredSubmission>> errorQueue = new LinkedBlockingQueue<>();

    public ExecutionManager(Path problems, URI negotiator)
    {
        this.problems = problems;
        this.executionThread = new ExecutionThread();
        this.reportThread = new ReportThread(negotiator);
        this.errorThread = new ErrorThread(negotiator);
    }

    /**
     * Enqueue a submission for execution.
     * 
     * @param submission the submission
     */
    public void enqueue(RegisteredSubmission submission) throws UnavailableExecutorException
    {
        log.debug("Enqueuing submission {}", submission);

        if (!ExecutorFactory.executorExists(submission))
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
        this.errorThread.start();
    }

    @Override
    public void close() throws IOException
    {
        log.info("Closing execution manager");
        this.executionThread.interrupt();
        this.reportThread.interrupt();
        this.errorThread.interrupt();
    }

    /**
     * Thread to perform the actual executions.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ExecutionThread extends Thread
    {
        public ExecutionThread()
        {
            super("Execution thread");
        }

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

                    /* We may not need to report an error, but here's one
                     * half-constructed and ready to go in the event we do. */
                    ErrorSubmission<RegisteredSubmission> error = new ErrorSubmission<>();
                    error.setId(submission.getId());
                    error.setStage(CapabilityType.EXECUTOR);
                    error.setSubmission(submission);

                    try
                    {
                        problem = Problems.problemByName(problems, submission.getProblem());
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to retrieve problem definition", e);

                        error.setType(ErrorType.E_INTERNAL);
                        error.setMessage("Failed to retrieve problem definition");
                        ExecutionManager.this.errorQueue.offer(error);

                        continue;
                    }

                    try
                    {
                        environment = ExecutionEnvironmentFactory.getExecutionEnvironment();
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to retrieve an execution environment", e);

                        error.setType(ErrorType.E_INTERNAL);
                        error.setMessage("Failed to retrieve an execution environment");
                        ExecutionManager.this.errorQueue.offer(error);

                        continue;
                    }

                    try
                    {
                        executor = ExecutorFactory.getExecutor(submission, environment);
                    }
                    catch (UnavailableExecutorException e)
                    {
                        log.error("No executor available for submission", e);

                        error.setType(ErrorType.E_INTERNAL);
                        error.setMessage("No executor available for submission");
                        ExecutionManager.this.errorQueue.offer(error);

                        continue;
                    }

                    try
                    {
                        executor.compile();
                    }
                    catch (IOException e)
                    {
                        log.error("Failure compiling submission", e);

                        error.setType(ErrorType.E_COMPILATION);
                        error.setMessage("Failure compiling submission");
                        ExecutionManager.this.errorQueue.offer(error);

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

                            error.setType(ErrorType.E_RUNTIME);
                            error.setMessage(e.getMessage());
                            ExecutionManager.this.errorQueue.offer(error);

                            continue;
                        }
                    }

                    Execution execution = new Execution(submission);
                    execution.setCases(executionCases);

                    ExecutionManager.this.outgoingQueue.offer(execution);

                    try
                    {
                        executor.close();
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to tear down executor", e);
                    }
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
     * Thread to report back to the negotiator.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ReportThread extends Thread
    {
        private final URI negotiator;

        public ReportThread(URI negotiator)
        {
            super("Negotiator reporting thread");
            this.negotiator = negotiator;
        }

        @Override
        public void run()
        {
            log.info("Starting negotiator reporting thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    Execution execution = ExecutionManager.this.outgoingQueue.take();

                    ObjectMapper mapper = new ObjectMapper();

                    HttpClient client;
                    HttpPost post;
                    HttpEntity body;

                    client = HttpClients.createDefault();
                    post = new HttpPost(this.negotiator.resolve("judge/" + execution.getId()));
                    try
                    {
                        body = new ByteArrayEntity(mapper.writeValueAsBytes(execution));
                    }
                    catch (JsonProcessingException e)
                    {
                        log.error("Failed to encode outgoing execution object to JSON", e);
                        continue;
                    }

                    post.setHeader("Content-Type", "application/json");
                    post.setEntity(body);

                    try
                    {
                        client.execute(post);
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to call negotiator with outgoing execution object", e);
                        continue;
                    }
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing negotiator reporting thread");
        }
    }

    /**
     * Thread to send errors back to the negotiator.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ErrorThread extends Thread
    {
        private final URI negotiator;

        public ErrorThread(URI negotiator)
        {
            super("Error reporting thread");
            this.negotiator = negotiator;
        }

        @Override
        public void run()
        {
            log.info("Starting error reporting thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    ErrorSubmission<RegisteredSubmission> error = ExecutionManager.this.errorQueue.take();

                    ObjectMapper mapper = new ObjectMapper();

                    HttpClient client;
                    HttpPost post;
                    HttpEntity body;

                    client = HttpClients.createDefault();
                    post = new HttpPost(this.negotiator.resolve("error/" + error.getId()));
                    try
                    {
                        body = new ByteArrayEntity(mapper.writeValueAsBytes(error));
                    }
                    catch (JsonProcessingException e)
                    {
                        log.error("Failed to encode outgoing error object to JSON", e);
                        continue;
                    }

                    post.setHeader("Content-Type", "application/json");
                    post.setEntity(body);

                    try
                    {
                        client.execute(post);
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to call negotiator with outgoing error object", e);
                        continue;
                    }
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing error reporting thread");
        }
    }
}