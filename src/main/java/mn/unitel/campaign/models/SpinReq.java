package mn.unitel.campaign.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
public class SpinReq {
    String phoneNo;
    UUID spinId;
}
