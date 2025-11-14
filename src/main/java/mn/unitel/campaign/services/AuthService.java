package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.clients.toki_user_info.TokiUserInfoRes;
import mn.unitel.campaign.models.LoginReq;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {
    Logger logger = Logger.getLogger(AuthService.class.getName());

    @Inject
    TokiService tokiService;

    @Inject
    JwtService jwtService;

    public Response login(LoginReq loginRequest) {
        try {
            String tokiId = tokiService.getTokiIdFromToken(loginRequest.getTokenId());

            TokiUserInfoRes tokiUserInfo = tokiService.getTokiUserInfo(tokiId);

            return Response.ok().entity(
                            new CustomResponse<>(
                                    "success",
                                    "Login successful",
                                    jwtService.generateTokenWithPhone(
                                            tokiUserInfo.getData().getNationalId(),
                                            tokiUserInfo.getData().getPhoneNo(),
                                            tokiId,
                                            tokiUserInfo.getData().getNationalId()
                                    )
                            )
                    )
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new CustomResponse<>("fail", "wrong toki token id", null)).build();
        }
    }
}
