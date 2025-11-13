package mn.unitel.campaign.clients.toki_user_info;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class TokiUserAuthRes {
    private String path;
    private String error;
    private String message;
    private int status;
    private TokiUserAuthDataObj data;
}
