package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import mn.unitel.campaign.clients.toki_general.TokiGeneralClient;
import mn.unitel.campaign.clients.toki_general.TokiGeneralInfo;
import mn.unitel.campaign.clients.toki_general.TokiNotiReq;
import mn.unitel.campaign.clients.toki_user_info.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TokiService {
    Logger logger = Logger.getLogger(TokiService.class);

    @RestClient
    TokiGeneralClient tokiGeneralClient;

    @RestClient
    TokiUserClient tokiUserClient;

    @ConfigProperty(name = "toki.user.auth.username")
    String tokiUserAuthUsername;

    @ConfigProperty(name = "toki.user.auth.password")
    String tokiUserAuthPassword;

    public String getTokiId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            logger.warn("getTokiId() called with empty or null national ID");
            return "NOT_FOUND";
        }

        TokiUserAuthRes authRes;
        try {
            authRes = tokiUserClient.getAuthUser(
                    TokiUserAuthReq.builder()
                            .username(tokiUserAuthUsername)
                            .password(tokiUserAuthPassword)
                            .build()
            );
        } catch (Exception e) {
            logger.errorf("Failed to authenticate with Toki API for national ID %s: %s", nationalId, e.getMessage());
            return "NOT_FOUND";
        }

        TokiUsersByNationalIdRes tokiUserInfo;
        try {
            tokiUserInfo = tokiUserClient.getInfoByNationalId(
                    "Bearer " + authRes.getData().getAccessToken(),
                    nationalId
            );
        } catch (Exception e) {
            logger.errorf("Failed to fetch user info for national ID %s: %s", nationalId, e.getMessage());
            return "NOT_FOUND";
        }

        if (tokiUserInfo == null || tokiUserInfo.getCustomers() == null) {
            logger.warnf("No customer data found for national ID: %s", nationalId);
            return "NOT_FOUND";
        }

        return tokiUserInfo.getCustomers().stream()
                .findFirst()
                .map(Customer::getAccountId)
                .orElseGet(() -> {
                    logger.warnf("Customer list empty for national ID: %s", nationalId);
                    return "NOT_FOUND";
                });
    }

    public void sendPushNoti(String tokiId, String title, String body) {
        logger.info("Sending toki noti to user: " + tokiId);
        String token = "Bearer " + tokiGeneralClient.getToken().getData().getAccessToken();

        try {
            tokiGeneralClient.send(
                    token,
                    TokiNotiReq.builder()
                            .title(title) // TODO Change
                            .body(body)
                            .url("https://link.toki.mn/VsQK") // TODO Change
                            .buttonName("OK") // TODO Change
                            .accountId(tokiId)
                            .icon("test")
                            .merchantId("66a71d8328f4dda2cd2b1d9d") // TODO Change
                            .build());
        } catch (Exception e) {
            logger.error("Failed to send push noti to Toki ID: " + tokiId + ", " + e.getMessage());
        }
    }

    public TokiUserInfoRes getTokiUserInfo(String tokiId) throws Exception {
        TokiUserAuthRes authRes = tokiUserClient.getAuthUser(
                TokiUserAuthReq.builder()
                        .username(tokiUserAuthUsername)
                        .password(tokiUserAuthPassword)
                        .build()
        );

        return tokiUserClient.getUserData(
                authRes.getData().getAccessToken(),
                TokiUserInfoReq.builder()
                        .requestId(tokiId)
                        .accountId(tokiId)
                        .build()
        );
    }

    public String getTokiIdFromToken(String token) throws Exception {
        token = "Bearer " + token;

        TokiGeneralInfo tokiGeneralInfo = tokiGeneralClient.getUserInfo(token);

        if (tokiGeneralInfo.getData().get_id().isBlank() || tokiGeneralInfo.getData().get_id() == null) {
            throw new Exception("Toki ID not found in token");
        }

        return tokiGeneralInfo.getData().get_id();
    }
}
