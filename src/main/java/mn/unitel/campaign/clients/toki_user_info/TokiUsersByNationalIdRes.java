package mn.unitel.campaign.clients.toki_user_info;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class TokiUsersByNationalIdRes {
    private List<Customer> customers;
}
