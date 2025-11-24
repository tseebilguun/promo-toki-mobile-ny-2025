package mn.unitel.campaign.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimedPrize {
    Integer prizeId;
    String coupon;
    String claimedPhoneNo;
}
