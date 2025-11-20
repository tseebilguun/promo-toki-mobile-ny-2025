package mn.unitel.campaign.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public class RemainingSpinRes extends BaseJsonModel {
    @JsonProperty
    LocalDateTime currentDate;
    @JsonProperty
    Integer remainingSpins;
    @JsonProperty
    String phoneNo;
    @JsonProperty
    String tokiId;
    @JsonProperty
    String nationalId;
    @JsonProperty
    List<ClaimedPrize> claimedPrizes;
    @JsonProperty
    UUID spinId;
    @JsonProperty
    Integer weekNumber;
}
