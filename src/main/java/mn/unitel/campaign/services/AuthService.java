package mn.unitel.campaign.services;

import DTO.DTO_response.FetchServiceDetailsResponse;
import Executable.APIUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.Helper;
import mn.unitel.campaign.clients.toki_user_info.TokiUserInfoRes;
import mn.unitel.campaign.models.LoginReq;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {
    Logger logger = Logger.getLogger(AuthService.class.getName());

    @Inject
    TokiService tokiService;

    @Inject
    JwtService jwtService;

    @ConfigProperty(name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    @Inject
    Helper helper;

    public Response login(LoginReq loginRequest) {
        if (loginRequest.getMsisdn() == null || loginRequest.getMsisdn().isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Утасны дугаар оруулна уу",
                                    null
                            )
                    )
                    .build();

        if (!helper.isTokiNumber(loginRequest.getMsisdn())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Хэрэглэгчийн бүртгэлтэй дугаар Toki Mobile биш байна",
                                    null
                            )
                    )
                    .build();
        }

        try {
            FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(loginRequest.getMsisdn(), debugMode);
            String accountName = servideDetails.getAccountName();

            String nationalId = helper.getNationalIdByPhoneNo(loginRequest.getMsisdn(), accountName);

            if (nationalId.equals("NOT_FOUND")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(
                                new CustomResponse<>(
                                        "fail",
                                        "Утасны дугаартай хэрэглэгчийн мэдээлэл олдсонгүй",
                                        null
                                )
                        )
                        .build();
            }

            String tokiId = tokiService.getTokiId(nationalId);

            if (tokiId.equals("NOT_FOUND")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(
                                new CustomResponse<>(
                                        "fail",
                                        "Хэрэглэгчийн Toki мэдээлэл олдсонгүй",
                                        null
                                )
                        )
                        .build();
            }

            TokiUserInfoRes tokiUserInfo = tokiService.getTokiUserInfo(tokiId);

            return Response.ok().entity(
                            new CustomResponse<>(
                                    "success",
                                    "Login successful",
                                    jwtService.generateTokenWithPhone(
                                            tokiUserInfo.getData().getNationalId(),
                                            tokiUserInfo.getData().getPhoneNo(),
                                            accountName,
                                            tokiId,
                                            tokiUserInfo.getData().getNationalId()
                                    )
                            )
                    )
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Утасны дугаар буруу байна",
                                    null
                            )
                    )
                    .build();
        }
    }
}
