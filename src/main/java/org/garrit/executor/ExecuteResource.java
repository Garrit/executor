package org.garrit.executor;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.garrit.common.messages.RegisteredSubmission;

/**
 * Expose execution functionality via HTTP.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Path("/execute")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExecuteResource
{
    private final ExecutionManager manager;

    public ExecuteResource(ExecutionManager manager)
    {
        this.manager = manager;
    }

    @POST
    public Response executeSubmission(RegisteredSubmission submission)
    {
        try
        {
            this.manager.enqueue(submission);
        }
        catch (UnavailableExecutorException e)
        {
            return Response.status(Status.NOT_IMPLEMENTED).build();
        }

        return Response.status(Status.ACCEPTED).build();
    }
}