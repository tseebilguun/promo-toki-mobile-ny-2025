package mn.unitel.campaign.models;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public class RemainingSpinRes extends BaseJsonModel {
    LocalDateTime currentDate;
    Integer remainingSpins;
    String phoneNo;
    String tokiId;
    String nationalId;
    List<ClaimedPrize> claimedPrizes;
    UUID spinId;
    Integer weekNumber;
}
