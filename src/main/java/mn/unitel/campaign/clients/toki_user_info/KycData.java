package mn.unitel.campaign.clients.toki_user_info;

import lombok.Data;

@Data
public class KycData {
    private String completeKyc;
    private String firstName;
    private String lastName;
    private String nationalId;
    private String phoneNo;
}