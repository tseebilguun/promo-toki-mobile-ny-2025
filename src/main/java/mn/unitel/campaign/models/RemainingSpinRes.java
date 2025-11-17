package mn.unitel.campaign.models;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@Builder
public class RemainingSpinRes extends BaseJsonModel {
    LocalDateTime currentDate;
    Integer remainingSpins;
    String phoneNo;
    String tokiId;
    String nationalId;
    UUID spinId;
}
