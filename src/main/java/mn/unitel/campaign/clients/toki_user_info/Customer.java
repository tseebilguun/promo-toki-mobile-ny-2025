package mn.unitel.campaign.clients.toki_user_info;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Customer {
    private String accountId;
    private String firstName;
    private String lastName;
    private String customerType;
    private String phoneNo;
}
