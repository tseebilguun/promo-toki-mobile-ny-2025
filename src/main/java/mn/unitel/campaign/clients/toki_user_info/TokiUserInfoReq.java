package mn.unitel.campaign.clients.toki_user_info;

import lombok.Builder;


@Builder
public record TokiUserInfoReq(String requestId,
                              String accountId){
}