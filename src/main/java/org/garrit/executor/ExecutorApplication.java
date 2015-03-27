package org.garrit.executor;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

import java.util.Map;

import org.garrit.common.messages.statuses.Status;

/**
 * Main entry point for the executor service.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class ExecutorApplication extends Application<ExecutorConfiguration>
{
    private Status status;
    private ExecutionManager executor;

    public static void main(String[] args) throws Exception
    {
        new ExecutorApplication().run(args);
    }

    @Override
    public String getName()
    {
        return ExecutorApplication.class.getName();
    }

    @Override
    public void run(ExecutorConfiguration config, Environment env) throws Exception
    {
        for (Map.Entry<String, String> executorEntry : config.getExecutors().entrySet())
            ExecutorFactory.registerExecutor(
                    executorEntry.getKey(),
                    Class.forName(executorEntry.getValue()).asSubclass(Executor.class));

        this.executor = new ExecutionManager(config.getProblems(), config.getNegotiator());

        this.status = new Status(config.getName());
        this.status.setCapabilityStatus(executor);

        final StatusResource statusResource = new StatusResource(this.status);
        final ExecuteResource executeResource = new ExecuteResource(this.executor);

        env.jersey().register(statusResource);
        env.jersey().register(executeResource);

        final StatusHealthCheck statusHealthCheck = new StatusHealthCheck(status);

        env.healthChecks().register("status", statusHealthCheck);

        this.executor.start();
    }
}