package org.garrit.executor;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.garrit.common.statuses.Status;

/**
 * Main entry point for the executor service.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class Executor extends Application<ExecutorConfiguration>
{
    private Status status;

    public static void main(String[] args) throws Exception
    {
        new Executor().run(args);
    }

    @Override
    public String getName()
    {
        return Executor.class.getName();
    }

    @Override
    public void initialize(Bootstrap<ExecutorConfiguration> bootstrap)
    {
    }

    @Override
    public void run(ExecutorConfiguration config, Environment env) throws Exception
    {
        this.status = new Status(config.getName());
        final StatusResource statusResource = new StatusResource(this.status);

        env.jersey().register(statusResource);

        final StatusHealthCheck statusHealthCheck = new StatusHealthCheck(status);

        env.healthChecks().register("status", statusHealthCheck);
    }
}