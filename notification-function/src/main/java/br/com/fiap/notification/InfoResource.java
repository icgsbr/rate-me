package br.com.fiap.notification;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Trivial endpoint so the scaffold module starts and is observable. */
@Path("/")
public class InfoResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String info() {
        return "notification-function scaffold - real trigger is Azure Monitor (cloud only)";
    }
}
