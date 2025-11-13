package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.jooq.tables.records.PrizeListRecord;
import mn.unitel.campaign.jooq.tables.records.SpecialPrizeRuleRecord;
import mn.unitel.campaign.jooq.tables.records.SpinEligibleNumbersRecord;
import mn.unitel.campaign.models.RemainingSpinRes;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static mn.unitel.campaign.jooq.Tables.SPECIAL_PRIZE_RULE;
import static mn.unitel.campaign.jooq.Tables.SPIN_ELIGIBLE_NUMBERS;

@ApplicationScoped
public class SpinnerService {
    Logger logger = Logger.getLogger(SpinnerService.class);

    @Inject
    DSLContext dsl;

    public Response getRemainingSpins(String msisdn) {
        try {
            List<SpinEligibleNumbersRecord> records = dsl
                    .selectFrom(SPIN_ELIGIBLE_NUMBERS)
                    .where(SPIN_ELIGIBLE_NUMBERS.PHONE_NO.eq(msisdn))
                    .and(SPIN_ELIGIBLE_NUMBERS.USED.eq(false))
                    .orderBy(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE.asc())
                    .fetch();

            logger.info("Total records : " + records.size());

            if (records.isEmpty()) {
                return Response.ok(
                                new CustomResponse<>(
                                        "success",
                                        "No remaining spins",
                                        RemainingSpinRes.builder()
                                                .currentDate(LocalDateTime.now())
                                                .remainingSpins(records.size())
                                                .phoneNo(msisdn)
                                                .spinId(null)
                                                .build()
                                )
                        )
                        .build();
            }

            logger.info(records.get(0).toString());

            SpinEligibleNumbersRecord firstRecord = records.get(0);

            return Response.ok(
                            new CustomResponse<>(
                                    "success",
                                    "Remaining spins fetched successfully",
                                    RemainingSpinRes.builder()
                                            .currentDate(LocalDateTime.now())
                                            .remainingSpins(records.size())
                                            .phoneNo(msisdn)
                                            .spinId(firstRecord.getId())
                                            .build()
                            )
                    )
                    .build();

        } catch (Exception e) {
            logger.info(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new CustomResponse<>("fail", "Internal error", null))
                    .build();
        }
    }

    public void spin(String msisdn) {
        List<SpecialPrizeRuleRecord> records = dsl.selectFrom(SPECIAL_PRIZE_RULE)
                .where(SPECIAL_PRIZE_RULE.CLAIMED.eq(false))
                .and(SPECIAL_PRIZE_RULE.MATCH_DATE.eq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))))
                .and(SPECIAL_PRIZE_RULE.MATCH_TIME.lessThan(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))))
                .orderBy(SPECIAL_PRIZE_RULE.MATCH_TIME.asc())
                .fetch();

        if (!records.isEmpty()) {
            logger.info("Available prizes for " + msisdn + ":");
            logger.info(records.toString());
        }
    }
}