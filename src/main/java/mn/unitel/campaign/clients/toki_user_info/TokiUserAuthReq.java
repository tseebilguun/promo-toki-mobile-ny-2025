package mn.unitel.campaign.clients.toki_user_info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class TokiUserAuthReq {
    @JsonProperty("username")
    String username;
    @JsonProperty("password")
    String password;
}
