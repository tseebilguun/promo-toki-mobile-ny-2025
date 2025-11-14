package mn.unitel.campaign.clients.toki_user_info;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import mn.unitel.campaign.filters.OutgoingRequestLoggingFilter;
import mn.unitel.campaign.filters.OutgoingResponseLoggingFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1/api")
@RegisterRestClient(configKey = "toki.user.info")
@RegisterProvider(OutgoingRequestLoggingFilter.class)
@RegisterProvider(OutgoingResponseLoggingFilter.class)
public interface TokiUserClient {
    @POST
    @Path("/payment/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    TokiUserAuthRes getAuthUser(TokiUserAuthReq req);

    @POST
    @Path("/payment/info/user")
    @Produces(MediaType.APPLICATION_JSON)
    TokiUserInfoRes getUserData(@HeaderParam("Authorization") String authorization, TokiUserInfoReq tokiUserInfoReq);

    @GET
    @Path("/payment/nationalId/{nationalId}")
    TokiUsersByNationalIdRes getInfoByNationalId(
            @HeaderParam("Authorization") String authorization,
            @PathParam("nationalId") String nationalId
    );
}
