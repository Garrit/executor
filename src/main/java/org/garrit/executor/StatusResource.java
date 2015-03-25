package org.garrit.executor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.garrit.common.messages.statuses.Status;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource
{
    private final Status status;

    public StatusResource(Status status)
    {
        this.status = status;
    }

    @GET
    public Status getStatus()
    {
        return this.status;
    }
}