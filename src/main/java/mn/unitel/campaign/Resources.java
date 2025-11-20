package mn.unitel.campaign;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.models.LoginReq;
import mn.unitel.campaign.models.SpinReq;
import mn.unitel.campaign.services.AuthService;
import mn.unitel.campaign.services.SpinnerService;

@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class Resources {
    @Inject
    AuthService authService;
    @Inject
    SpinnerService spinnerService;

    @POST
    @Path("/auth/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginReq loginRequest) {
        return authService.login(loginRequest);
    }

    @GET
    @Path("/spinnger/getInfo")
    public Response getGeneralInfo(@Context ContainerRequestContext ctx) {
        return spinnerService.getGemeralInfo(ctx);
    }

    @POST
    @Path("spinner/spin")
    public Response spin(SpinReq spinReq, @Context ContainerRequestContext ctx) {
        return spinnerService.spin(spinReq, ctx);
    }
}
