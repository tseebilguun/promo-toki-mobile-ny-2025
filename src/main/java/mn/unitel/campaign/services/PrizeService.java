package mn.unitel.campaign.services;

import Executable.APIUtil;
import Executable.PhoneInfo;
import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.context.api.NamedInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mn.unitel.campaign.Utils;
import mn.unitel.campaign.jooq.tables.records.PrizeListRecord;
import mn.unitel.campaign.jooq.tables.records.SpinEligibleNumbersRecord;
import mn.unitel.campaign.legacy.SmsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static mn.unitel.campaign.jooq.Tables.PRIZE_LIST;
import static mn.unitel.campaign.jooq.Tables.SPIN_ELIGIBLE_NUMBERS;

@ApplicationScoped
public class PrizeService {
    Logger logger = Logger.getLogger(PrizeService.class);

    @Inject
    DSLContext dsl;

    @Inject
    @NamedInstance("prize-executor")
    ManagedExecutor prizeExecutor;

    @ConfigProperty(name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    @Inject
    SmsService smsService;

    @Inject
    TokiService tokiService;

    @ConfigProperty(name = "test.now")
    LocalDateTime testNow;

    public void processPrizeAsync(int prizeId, String msisdn, String nationalId, String tokiId, UUID spinId, String coupon) {
        prizeExecutor.execute(() -> {
            logger.info("Executing Prize Async Request at " + Thread.currentThread().getName());
            logger.info("Prize ID: " + prizeId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                switch (prizeId) {
                    case 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114 ->
                            processPhysicalPrize(prizeId, nationalId, msisdn, spinId, tokiId);
                    case 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217 ->
                            processCouponPrize(prizeId, nationalId, msisdn, spinId, tokiId, coupon);
                    case 301, 302, 303 -> processDataPrize(prizeId, nationalId, msisdn, spinId, tokiId);
                    case 304, 305 -> processRecurringDataPrize(prizeId, nationalId, msisdn, spinId, tokiId);
                    default -> { /* ignore invalid prize IDs */ }
                }
            } catch (Exception e) {
                logger.info("Error processing prize asynchronously:");
                logger.info(e.getMessage());
            }
        });
    }

    private void processPhysicalPrize(int prizeId, String nationalId, String msisdn, UUID spinId, String tokiId) {
        logger.info("Processing physical prize: " + prizeId + " for MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId + ", Toki ID: " + tokiId);

        SpinEligibleNumbersRecord spinRecord = dsl.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .limit(1)
                .fetchOne();

        PrizeListRecord prizeRecord = dsl.selectFrom(PRIZE_LIST)
                .where(PRIZE_LIST.ID.eq(prizeId))
                .limit(1)
                .fetchOne();

        if (spinRecord == null) {
            logger.info("Spin record is not found for processing physical prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        if (!spinRecord.getNationalId().equalsIgnoreCase(nationalId) && spinRecord.getId().equals(spinId)) {
            logger.info("National ID does not match for processing physical prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        if (prizeRecord == null) {
            logger.info("Prize record is not found for processing physical prize. Prize ID: " + prizeId);
            return;
        }

        dsl.update(SPIN_ELIGIBLE_NUMBERS)
                .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, true)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .execute();

        smsService.send("4477", msisdn, "Chamd " + prizeRecord.getPrizeName() + " beleglej baina. Central Tower-n 8 davhraas irj belgee avaarai.", true);
        tokiService.sendPushNoti(tokiId, "55 БЭЛЭГТЭЙ ШИНЭ ЖИЛ", "Toki Mobile-д нэгдэж " + prizeRecord.getPrizeName() + "-н эзэн боллоо. Баяр хүргэе \uD83C\uDF89");
    }

    private void processCouponPrize(int prizeId, String nationalId, String msisdn, UUID spinId, String tokiId, String coupon) {
        logger.info("Processing coupon prize: " + prizeId + " for MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId + ", Toki ID: " + tokiId);

        SpinEligibleNumbersRecord spinRecord = dsl.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .limit(1)
                .fetchOne();

        PrizeListRecord prizeRecord = dsl.selectFrom(PRIZE_LIST)
                .where(PRIZE_LIST.ID.eq(prizeId))
                .limit(1)
                .fetchOne();

        if (spinRecord == null) {
            logger.info("Spin record is not found for processing physical prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        if (!spinRecord.getNationalId().equalsIgnoreCase(nationalId) && spinRecord.getId().equals(spinId)) {
            logger.info("National ID does not match for processing physical prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        if (prizeRecord == null) {
            logger.info("Prize record is not found for processing physical prize. Prize ID: " + prizeId);
            return;
        }

        dsl.update(SPIN_ELIGIBLE_NUMBERS)
                .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, true)
                .set(SPIN_ELIGIBLE_NUMBERS.COUPON, coupon)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .execute();

        smsService.send("4477", msisdn, "Chamd " + prizeRecord.getPrizeName() + " beleglej baina. 'Minii beleg' hesgees coupon code-oo haraarai. https://link.toki.mn/", true); // TODO deeplink
        tokiService.sendPushNoti(tokiId, "55 БЭЛЭГТЭЙ ШИНЭ ЖИЛ", "Toki Mobile-д нэгдэж " + prizeRecord.getPrizeName() + "-н эзэн боллоо. Баяр хүргэе \uD83C\uDF89 'Миний бэлэг' хэсгээс купон кодоо хараарай.");
    }

    private void processDataPrize(int prizeId, String nationalId, String msisdn, UUID spinId, String tokiId) {
        logger.info("Processing data prize: " + prizeId + " for MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId + ", Toki ID: " + tokiId);
        SpinEligibleNumbersRecord spinRecord = dsl.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .limit(1)
                .fetchOne();

        if (spinRecord == null) {
            logger.info("Spin record is not found for processing data prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        LocalDateTime expireDate;
        String dataAmount;
        String dataAmountText;
        Integer dataDuration;
        String expireDateStr;

        switch (prizeId) {
            case 301 -> {
                dataAmount = "3221225472";
                expireDate = LocalDateTime.now()
                        .plusDays(1)
                        .withHour(23)
                        .withMinute(59)
                        .withSecond(58);
                dataAmountText = "3GB";
                dataDuration = 2;
            }
            case 302 -> {
                dataAmount = "5905580032";
                expireDate = LocalDateTime.now()
                        .plusDays(4)
                        .withHour(23)
                        .withMinute(59)
                        .withSecond(58);
                dataAmountText = "5.5GB";
                dataDuration = 5;
            }
            case 303 -> {
                dataAmount = "59055800320";
                expireDate = LocalDateTime.now()
                        .plusDays(9)
                        .withHour(23)
                        .withMinute(59)
                        .withSecond(58);
                dataAmountText = "55GB";
                dataDuration = 10;
            }
            default -> {
                logger.info("Invalid prize id: " + prizeId);
                return;
            }
        }

        expireDateStr = expireDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        JsonNode addProductRes = Utils.toJsonNode(APIUtil.addDeleteProduct(msisdn, "campaign_data", dataAmount, "A25_063", expireDateStr, "Campaign", "add", debugMode));

        if (addProductRes == null || !addProductRes.path("result").asText().equals("success")) {
            logger.info("Failed to add product for data prize. Prize ID: " + prizeId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId);
            dsl.update(SPIN_ELIGIBLE_NUMBERS)
                    .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                    .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                    .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, false)
                    .set(SPIN_ELIGIBLE_NUMBERS.RESPONSE, addProductRes.toString())
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_DURATION, dataDuration)
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_VOLUME, dataAmount)
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_EXPIRE_DATE, expireDate)
                    .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                    .execute();
        } else {
            dsl.update(SPIN_ELIGIBLE_NUMBERS)
                    .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                    .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                    .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, true)
                    .set(SPIN_ELIGIBLE_NUMBERS.RESPONSE, addProductRes.toString())
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_DURATION, dataDuration)
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_VOLUME, dataAmount)
                    .set(SPIN_ELIGIBLE_NUMBERS.PACKAGE_EXPIRE_DATE, expireDate)
                    .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                    .execute();

            smsService.send("4477", msisdn, expireDateStr.substring(0, 4) + "/" +
                            expireDateStr.substring(4, 6) + "/" +
                            expireDateStr.substring(6, 8) + " hurtel ashiglah shine jiliin uramshuulliin " + dataAmountText + " data idevhejlee."
                    , true);

            tokiService.sendPushNoti(tokiId, "Toki Mobile", "Дугаарт " + dataAmountText + " дата идэвхэжлээ.");
        }
    }

    private void processRecurringDataPrize(int prizeId, String nationalId, String msisdn, UUID spinId, String tokiId) {
        logger.info("Processing recurring data prize: " + prizeId + " for MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId + ", Toki ID: " + tokiId);
        SpinEligibleNumbersRecord spinRecord = dsl.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                .limit(1)
                .fetchOne();

        if (spinRecord == null) {
            logger.info("Spin record is not found for processing data prize. Spin ID: " + spinId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Prize ID: " + prizeId);
            return;
        }

        String dataAmountStr;
        String dataAmount;
        LocalDateTime expireDate = LocalDateTime.now()
                .plusMonths(4)
                .with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23)
                .withMinute(59)
                .withSecond(58);
        String expireDateStr = expireDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));


        switch (prizeId) {
            case 304 -> {
                dataAmountStr = "5GB";
                dataAmount = "5368709120";
            }
            case 305 -> {
                dataAmountStr = "11GB";
                dataAmount = "11811160064";
            }
            default -> {
                logger.info("Invalid prize id: " + prizeId);
                return;
            }
        }

        JsonNode addProductRes = Utils.toJsonNode(APIUtil.addDeleteProduct(msisdn, "promo_data_recurring_custom_offer", dataAmount, "A25_063", expireDateStr, "Campaign", "add", debugMode));

        if (addProductRes == null || !addProductRes.path("result").asText().equals("success")) {
            logger.info("Failed to add product for data prize. Prize ID: " + prizeId + ", MSISDN: " + msisdn + ", National ID: " + nationalId + ", Spin Eligible ID: " + spinId);
            dsl.update(SPIN_ELIGIBLE_NUMBERS)
                    .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                    .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                    .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, false)
                    .set(SPIN_ELIGIBLE_NUMBERS.RESPONSE, addProductRes.toString())
                    .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                    .execute();
        } else {
            dsl.update(SPIN_ELIGIBLE_NUMBERS)
                    .set(SPIN_ELIGIBLE_NUMBERS.SPIN_DATE, debugMode ? testNow : LocalDateTime.now())
                    .set(SPIN_ELIGIBLE_NUMBERS.PRIZE_ID, prizeId)
                    .set(SPIN_ELIGIBLE_NUMBERS.SUCCESS, true)
                    .set(SPIN_ELIGIBLE_NUMBERS.RESPONSE, addProductRes.toString())
                    .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinId))
                    .execute();

            smsService.send("4477", msisdn, "Shine jiliin uramshuulliin " +
                            expireDateStr.substring(0, 4) + "/" +
                            expireDateStr.substring(4, 6) + "/" +
                            expireDateStr.substring(6, 8) + " hurtel ashiglah " + dataAmountStr + " idevhejlee."
                    , true);

            tokiService.sendPushNoti(tokiId, "Toki Mobile", "Дугаарт " + dataAmountStr + " дата идэвхэжлээ");
        }
    }
}
