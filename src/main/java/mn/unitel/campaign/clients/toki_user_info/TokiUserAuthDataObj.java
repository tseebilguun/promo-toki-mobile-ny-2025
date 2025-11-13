package mn.unitel.campaign.clients.toki_user_info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TokiUserAuthDataObj {
    @JsonProperty("AccessToken")
    private String accessToken;
}
