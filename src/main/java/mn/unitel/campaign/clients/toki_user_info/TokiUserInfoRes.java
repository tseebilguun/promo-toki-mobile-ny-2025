package mn.unitel.campaign.clients.toki_user_info;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokiUserInfoRes {
    private KycData data;
    private String message;
    private int status;
}
