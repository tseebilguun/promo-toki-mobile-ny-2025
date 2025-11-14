package mn.unitel.campaign;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.models.LoginReq;
import mn.unitel.campaign.services.AuthService;

@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class Resources {
    @Inject
    AuthService authService;

    @POST
    @Path("/auth/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginReq loginRequest) {
        return authService.login(loginRequest);
    }

    // Front end will call this API with the jwt token that was received from /auth/login
    @GET
    @Path("/spinnger/getInfo")
    public Response getGeneralInfo() {

    }
}
