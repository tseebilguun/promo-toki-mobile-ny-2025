package mn.unitel.campaign.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClaimedPrize {
    Integer prizeId;
    String coupon;
    String claimedPhoneNo;
    LocalDateTime claimedDate;
}
