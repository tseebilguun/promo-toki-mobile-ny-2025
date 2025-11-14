package mn.unitel.campaign.clients.toki_general;

import lombok.Data;

@Data
public class TokiGeneralInfo {
    private int statusCode;
    private String message;
    private UserData data;
    private String type;
}
