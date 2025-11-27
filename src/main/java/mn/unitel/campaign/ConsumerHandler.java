package mn.unitel.campaign;

import DTO.DTO_response.FetchServiceDetailsResponse;
import Executable.APIUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.unitel.campaign.legacy.SmsService;
import mn.unitel.campaign.services.TokiService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDateTime;

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

    @Inject
    TokiService tokiService;


    public void gotActive(String msisdn) {
        if (!helper.isTokiNumber(msisdn)) {
            logger.infof("Number %s is not a Toki number. Skipping.", msisdn);
            return;
        }

        FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(msisdn, debugMode);
        String accountName = servideDetails.getAccountName();

        String nationalId = helper.getNationalIdByPhoneNo(msisdn, accountName);

        if (nationalId.equals("NOT_FOUND")) {
            logger.infof("National ID not found for number %s. Skipping spin grant.", msisdn);
            return;
        }

        grantSpin(msisdn, accountName, nationalId, "New Number");
    }

    public void onRecharge(String msisdn) {
        if (!helper.isTokiNumber(msisdn)) {
            logger.infof("Number %s is not a Toki number. Skipping.", msisdn);
            return;
        }

        FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(msisdn, debugMode);
        String accountName = servideDetails.getAccountName();

        String nationalId = helper.getNationalIdByPhoneNo(msisdn, accountName);

        if (nationalId.equals("NOT_FOUND")) {
            logger.infof("National ID not found for number %s. Skipping spin grant.", msisdn);
            return;
        }

        grantSpin(msisdn, accountName, nationalId, "Recharge");
    }

    public void grantSpin(String msisdn, String accountName, String nationalId, String rechargeType) {
        dsl.insertInto(SPIN_ELIGIBLE_NUMBERS)
                .set(SPIN_ELIGIBLE_NUMBERS.NATIONAL_ID, nationalId)
                .set(SPIN_ELIGIBLE_NUMBERS.PHONE_NO, msisdn)
                .set(SPIN_ELIGIBLE_NUMBERS.ACCOUNT_NAME, accountName)
                .set(SPIN_ELIGIBLE_NUMBERS.RECHARGE_TYPE, rechargeType)
                .set(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE, LocalDateTime.now())
                .execute();

        if (rechargeType.equalsIgnoreCase("New Number"))
            smsService.send("4477", msisdn, "Toki Mobile-d negdej '55 Belegtei shine jil'-d oroltson " +
                    "beleg neeh erhtei bolloo. Mobile tsesiin uramshuulliin " +
                    "banner deer darj orno uu: https://link.toki.mn/CX5z", true);
        else if (rechargeType.equalsIgnoreCase("Recharge"))
            smsService.send("4477", msisdn, "'55 Belegtei shine jil'-d oroltson beleg neeh erhtei bolloo. " +
                    "Mobile tsesiin uramshuulliin banner deer darj orno uu: " +
                    "https://link.toki.mn/CX5z", true);
        else if (rechargeType.equalsIgnoreCase("Invitation"))
            logger.info("Granted spin by inviting friend: MSISDN: " + msisdn + ", National ID: " + nationalId);
    }
}
