package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import mn.unitel.campaign.clients.toki_noti.TokiNotiClient;
import mn.unitel.campaign.clients.toki_noti.TokiNotiReq;
import mn.unitel.campaign.clients.toki_user_info.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TokiService {
    Logger logger = Logger.getLogger(TokiService.class);

    @RestClient
    TokiNotiClient tokiNotiClient;

    @RestClient
    TokiUserClient tokiUserClient;

    @ConfigProperty(name = "toki.user.auth.username")
    String tokiUserAuthUsername;

    @ConfigProperty(name = "toki.user.auth.password")
    String tokiUserAuthPassword;

    public String getTokiId(String rd) {
        if (rd == null || rd.isBlank()) {
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
            logger.errorf("Failed to authenticate with Toki API for national ID %s: %s", rd, e.getMessage());
            return "NOT_FOUND";
        }

        TokiUserInfoRes tokiUserInfo;
        try {
            tokiUserInfo = tokiUserClient.getInfoByNationalId(
                    "Bearer " + authRes.getData().getAccessToken(),
                    rd
            );
        } catch (Exception e) {
            logger.errorf("Failed to fetch user info for national ID %s: %s", rd, e.getMessage());
            return "NOT_FOUND";
        }

        if (tokiUserInfo == null || tokiUserInfo.getCustomers() == null) {
            logger.warnf("No customer data found for national ID: %s", rd);
            return "NOT_FOUND";
        }

        return tokiUserInfo.getCustomers().stream()
                .findFirst()
                .map(Customer::getAccountId)
                .orElseGet(() -> {
                    logger.warnf("Customer list empty for national ID: %s", rd);
                    return "NOT_FOUND";
                });
    }

    public void sendPushNoti(String tokiId, String body) {
        logger.info("Sending toki noti to user: " + tokiId);

        String token = "Bearer " + tokiNotiClient.getToken().getData().getAccessToken();

        try {
            tokiNotiClient.send(
                    token,
                    TokiNotiReq.builder()
                            .title("Toki Mobile") // TODO Change
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
}
