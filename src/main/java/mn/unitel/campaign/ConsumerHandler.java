package mn.unitel.campaign;

import DTO.DTO_response.FetchServiceDetailsResponse;
import Executable.APIUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.unitel.campaign.jooq.tables.records.SpecialPrizeRuleRecord;
import mn.unitel.campaign.legacy.SmsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
public class ConsumerHandler {
    Logger logger = Logger.getLogger(ConsumerHandler.class);

    @Inject
    Helper helper;

    @Inject
    DSLContext dsl;

    @Inject
    SmsService smsService;

    @ConfigProperty(name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    public void gotActive(String msisdn) {
        if (!helper.isTokiNumber(msisdn)){
            logger.infof("Number %s is not a Toki number. Skipping.", msisdn);
            return;
        }

        FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(msisdn, debugMode);
        String accountName = servideDetails.getAccountName();
    }

    public void onRecharge(String msisdn) {
        if (!helper.isTokiNumber(msisdn)){
            logger.infof("Number %s is not a Toki number. Skipping.", msisdn);
            return;
        }

        FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(msisdn, debugMode);
        String accountName = servideDetails.getAccountName();
    }

    public void grantSpin(String msisdn, String accountName, String rechargeType) {
        dsl.insertInto(SPIN_ELIGIBLE_NUMBERS)
                .set(SPIN_ELIGIBLE_NUMBERS.PHONE_NO, msisdn)
                .set(SPIN_ELIGIBLE_NUMBERS.ACCOUNT_NAME, accountName)
                .set(SPIN_ELIGIBLE_NUMBERS.RECHARGE_TYPE, rechargeType)
                .set(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE, LocalDateTime.now())
                .execute();

        smsService.send("4477", msisdn, "", true);
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
