package mn.unitel.campaign.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import mn.unitel.campaign.ConsumerHandler;
import mn.unitel.campaign.helpers.RechargeNoti;
import mn.unitel.campaign.helpers.SmsNoti;
import mn.unitel.campaign.helpers.StatusNoti;
import mn.unitel.campaign.rabbitmq.util.RabbitMQSmsMessage;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

@Slf4j
@ApplicationScoped
public class Consumers {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CampaignConfig campaignConfig;

    @Inject
    ConsumerHandler consumerHandler;

    private static final Logger logger = Logger.getLogger(Consumers.class.getName());

    public void onStatusChange(byte[] bytes) {
        try {
            JsonNode json = objectMapper.readTree(bytes);
            logger.info("Status Change received");

            StatusNoti statusNoti = new StatusNoti(json);
            logger.info(statusNoti.toString());

            String msisdn = statusNoti.getPhoneNo();

            if (!campaignConfig.shouldProcess(msisdn)){
                logger.infof("%s is not a test number", msisdn);
                return;
            }


            if (!campaignConfig.isWithinCampaignPeriod(LocalDateTime.now())){
                logger.info("Campaign period is over");
                return;
            }

            if (!(statusNoti.getFormerStatus().equals("Pending Activation") && statusNoti.getCurrentStatus().equals("Active")))
                return;


//            consumerHandler.gotActive(msisdn);

        } catch (Exception e) {
            logger.error("JSON parsing failed", e);
        }
    }

    public void onRecharge(byte[] bytes) {
        try {
            JsonNode json = objectMapper.readTree(bytes);
            logger.info("Child offer creation received");

            RechargeNoti rechargeNoti = new RechargeNoti(json);
            logger.info(rechargeNoti.toString());

            String msisdn = rechargeNoti.getRechargedNumber();

            if (!campaignConfig.shouldProcess(msisdn)){
                logger.infof("%s is not a test number", msisdn);
                return;
            }


            if (!campaignConfig.isWithinCampaignPeriod(LocalDateTime.now())){
                logger.info("Campaign period is over");
                return;
            }

            if (rechargeNoti.getEntryLevelOffer().matches("34110|34111")) { //TODO Change
//                consumerHandler.onRecharge(msisdn);
            }

        } catch (Exception e) {
            logger.error("JSON parsing failed", e);
        }
    }
}
