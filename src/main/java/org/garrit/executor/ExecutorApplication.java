package org.garrit.executor;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

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
        this.executor = new ExecutionManager(config.getProblems());

        this.status = new Status(config.getName());
        this.status.setCapabilityStatus(executor);

        final StatusResource statusResource = new StatusResource(this.status);

        env.jersey().register(statusResource);

        final StatusHealthCheck statusHealthCheck = new StatusHealthCheck(status);

        env.healthChecks().register("status", statusHealthCheck);
    }
}