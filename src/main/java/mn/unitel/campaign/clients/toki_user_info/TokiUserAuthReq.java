package mn.unitel.campaign.clients.toki_user_info;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class TokiUserAuthReq {
    String username;
    String password;
}
