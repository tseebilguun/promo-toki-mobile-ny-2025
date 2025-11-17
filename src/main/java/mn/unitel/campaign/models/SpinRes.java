package mn.unitel.campaign.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpinRes extends BaseJsonModel {
    Integer prizeId;
    String prizeName;
    Boolean isSpecial;
    String coupon;
}
