package mn.unitel.campaign.services;

import DTO.DTO_response.FetchServiceDetailsResponse;
import Executable.APIUtil;
import io.smallrye.context.api.NamedInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.ConsumerHandler;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.Helper;
import mn.unitel.campaign.jooq.tables.records.SpinEligibleNumbersRecord;
import mn.unitel.campaign.legacy.SmsService;
import mn.unitel.campaign.models.InvitationReq;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.util.UUID;

import static mn.unitel.campaign.jooq.Tables.SPIN_ELIGIBLE_NUMBERS;

@ApplicationScoped
public class InvitationService {
    Logger logger = Logger.getLogger(PrizeService.class);

    @Inject
    DSLContext dsl;

    @Inject
    TokiService tokiService;

    @Inject
    Helper helper;
    @Inject
    ConsumerHandler consumerHandler;


    public Response sendInvite(InvitationReq req, @Context ContainerRequestContext ctx) {
        String nationalId = (String) ctx.getProperty("jwt.nationalId");
        String tokiId = (String) ctx.getProperty("jwt.tokiId");
        String phoneNo = (String) ctx.getProperty("jwt.phone");
        String accountName = (String) ctx.getProperty("jwt.accountName");

        logger.info("Invitation request received from nationalId: " + nationalId + ", msisdn: " + phoneNo + " accountName: " + accountName + ", toki ID: " + tokiId + ", inviting msisdn: " + req.getInvitedMsisdn());

        if (req.getInvitedMsisdn() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Toki Mobile-н 55-тай дугаар оруулна уу.",
                                    null
                            )
                    )
                    .build();
        }

        if (!helper.isTokiNumber(req.getInvitedMsisdn())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Toki Mobile-н 55-тай дугаар оруулна уу.",
                                    null
                            )
                    )
                    .build();
        }

        SpinEligibleNumbersRecord record = dsl.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                .where(SPIN_ELIGIBLE_NUMBERS.PHONE_NO.eq(req.getInvitedMsisdn()))
                .and(SPIN_ELIGIBLE_NUMBERS.RECHARGE_TYPE.eq("New Number"))
                .limit(1)
                .fetchOne();

        if (record == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Шалгуур хангаагүй дугаар байна. Зөвхөн шинэ хэрэглэгчийг урих боломжтой.",
                                    null
                            )
                    )
                    .build();
        }

        if (record.getInvitedBy() != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Уригдсан хэрэглэгч байна. Өөр дугаар оруулна уу.",
                                    null
                            )
                    )
                    .build();
        }

        UUID newNumberId = record.getId();

        int updatedRecord = dsl.update(SPIN_ELIGIBLE_NUMBERS)
                .set(SPIN_ELIGIBLE_NUMBERS.INVITED_BY, nationalId)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(newNumberId))
                .execute();

        if (updatedRecord == 0) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Алдаа гарлаа. Түр хүлээгээд дахин оролдоно уу.",
                                    null
                            )
                    )
                    .build();
        }


        try {
            consumerHandler.grantSpin(phoneNo, accountName, nationalId, "Invitation");

            return Response.ok()
                    .entity(
                            new CustomResponse<>(
                                    "success",
                                    "Найзаа урьж бэлэг авах 1 эрхтэй боллоо \uD83C\uDF89",
                                    null
                            )
                    )
                    .build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to process invitation for invited number %s by inviter %s", req.getInvitedMsisdn(), nationalId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                            new CustomResponse<>(
                                    "fail",
                                    "Алдаа гарлаа. Түр хүлээгээд дахин оролдоно уу.",
                                    null
                            )
                    )
                    .build();
        }
    }
}
