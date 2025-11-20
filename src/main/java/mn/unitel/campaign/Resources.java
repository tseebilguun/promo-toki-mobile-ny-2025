package mn.unitel.campaign;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.models.InvitationReq;
import mn.unitel.campaign.models.LoginReq;
import mn.unitel.campaign.models.SpinReq;
import mn.unitel.campaign.services.AuthService;
import mn.unitel.campaign.services.InvitationService;
import mn.unitel.campaign.services.SpinnerService;

@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class Resources {
    @Inject
    AuthService authService;

    @Inject
    SpinnerService spinnerService;

    @Inject
    InvitationService invitationService;
    @Inject
    ConsumerHandler consumerHandler;

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

    @POST
    @Path("invite/send")
    public Response sendInvite(InvitationReq req, @Context ContainerRequestContext ctx) {
        return invitationService.sendInvite(req, ctx);
    }

    @POST
    @Path("test/active")
    public Response testActive(@QueryParam("msisdn") String msisdn) {
        consumerHandler.gotActive(msisdn);
        return Response.ok().build();
    }
}
