package mn.unitel.campaign.clients.toki_user_info;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import mn.unitel.campaign.filters.OutgoingRequestLoggingFilter;
import mn.unitel.campaign.filters.OutgoingResponseLoggingFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "toki.user.info")
@RegisterProvider(OutgoingRequestLoggingFilter.class)
@RegisterProvider(OutgoingResponseLoggingFilter.class)
public interface TokiUserClient {
    @POST
    @Path("/v1/api/payment/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    TokiUserAuthRes getAuthUser(TokiUserAuthReq req);

    @GET
    @Path("/v1/api/payment/nationalId/{nationalId}")
    TokiUserInfoRes getInfoByNationalId(
            @HeaderParam("Authorization") String authorization,
            @PathParam("nationalId") String nationalId
    );
}
