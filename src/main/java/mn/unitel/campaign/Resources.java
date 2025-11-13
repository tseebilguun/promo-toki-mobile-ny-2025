package mn.unitel.campaign;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.services.SpinnerService;

@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class Resources {
    @Inject
    SpinnerService spinnerService;

    @POST
    @Path("/helloWorld")
    public Response helloWorld() {
        return Response.ok("Hello, World").build();
    }
}
