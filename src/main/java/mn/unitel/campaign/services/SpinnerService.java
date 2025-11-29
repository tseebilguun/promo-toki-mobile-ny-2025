package mn.unitel.campaign.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.CustomResponse;
import mn.unitel.campaign.Helper;
import mn.unitel.campaign.jooq.tables.records.PrizeListRecord;
import mn.unitel.campaign.jooq.tables.records.SpecialPrizeRuleRecord;
import mn.unitel.campaign.jooq.tables.records.SpinEligibleNumbersRecord;
import mn.unitel.campaign.models.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static mn.unitel.campaign.jooq.Tables.*;

@ApplicationScoped
public class SpinnerService {
    Logger logger = Logger.getLogger(SpinnerService.class);

    @Inject
    DSLContext dsl;

    @Inject
    Helper helper;

    @ConfigProperty(name = "physical.special.prize.ids")
    List<Integer> physicalPrizeIds;

    @ConfigProperty(name = "coupon.special.prize.ids")
    List<Integer> couponPrizeIds;

    @Inject
    PrizeService prizeService;

    @ConfigProperty(name = "test.now")
    LocalDateTime testNow;

    @ConfigProperty(name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    private static final Map<Integer, int[][]> WEEK_RANGES = Map.of(
            1, new int[][]{
                    {30, 301},
                    {50, 302},
                    {55, 303},
                    {85, 304},
                    {100, 305}
            },
            2, new int[][]{
                    {10, 303},
                    {60, 304},
                    {100, 305}
            },
            3, new int[][]{
                    {10, 303},
                    {60, 304},
                    {100, 305}
            },
            4, new int[][]{
                    {20, 302},
                    {25, 303},
                    {70, 304},
                    {100, 305}
            },
            5, new int[][]{
                    {30, 301},
                    {50, 302},
                    {55, 303},
                    {85, 304},
                    {100, 305}
            }
    );

    public Response getGemeralInfo(@Context ContainerRequestContext ctx) {
        try {
            String phoneNo = (String) ctx.getProperty("jwt.phone");
            String tokiId = (String) ctx.getProperty("jwt.tokiId");
            String accountName = (String) ctx.getProperty("jwt.accountName");
            String nationalId = (String) ctx.getProperty("jwt.nationalId");

            logger.info("JWT parsed info - Phone: " + phoneNo + ", TokiId: " + tokiId + ", AccountName: " + accountName + ", NationalId: " + nationalId);

            if (phoneNo == null || tokiId == null || nationalId == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                        .build();
            }

            List<SpinEligibleNumbersRecord> records = dsl
                    .selectFrom(SPIN_ELIGIBLE_NUMBERS)
                    .where(SPIN_ELIGIBLE_NUMBERS.NATIONAL_ID.eq(nationalId))
                    .and(SPIN_ELIGIBLE_NUMBERS.USED.eq(false))
                    .orderBy(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE.asc())
                    .fetch();

            logger.info("Total records of unused spins: " + records.size());

            List<SpinEligibleNumbersRecord> claimedPrizes = dsl
                    .selectFrom(SPIN_ELIGIBLE_NUMBERS)
                    .where(SPIN_ELIGIBLE_NUMBERS.NATIONAL_ID.eq(nationalId))
                    .and(SPIN_ELIGIBLE_NUMBERS.USED.eq(true))
                    .orderBy(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE.desc())
                    .fetch();

            List<ClaimedPrize> claimedPrizeList = claimedPrizes.stream()
                    .map(r -> ClaimedPrize.builder()
                            .prizeId(r.getPrizeId())
                            .coupon(r.getCoupon())
                            .claimedPhoneNo(r.getPhoneNo())
                            .claimedDate(r.getSpinDate())
                            .build()
                    )
                    .toList();


//             TODO Test change
            LocalDateTime now = debugMode ? testNow : LocalDateTime.now();
            Integer weekNumber = helper.getCurrentWeekNumber(now);

            if (records.isEmpty()) {
                return Response.ok(
                                new CustomResponse<>(
                                        "success",
                                        "Танд хүрд эргүүлэх эрх байхгүй байна.",
                                        RemainingSpinRes.builder()
                                                .currentDate(now)
                                                .remainingSpins(records.size())
                                                .phoneNo(phoneNo)
                                                .tokiId(tokiId)
                                                .nationalId(nationalId)
                                                .claimedPrizes(claimedPrizeList)
                                                .spinId(null)
                                                .weekNumber(weekNumber)
                                                .build()
                                )
                        )
                        .build();
            }

            logger.info(records.get(0).formatJSON());

            SpinEligibleNumbersRecord firstRecord = records.get(0);

            return Response.ok(
                            new CustomResponse<>(
                                    "success",
                                    "Remaining spins fetched successfully",
                                    RemainingSpinRes.builder()
                                            .currentDate(now)
                                            .remainingSpins(records.size())
                                            .phoneNo(phoneNo)
                                            .tokiId(tokiId)
                                            .nationalId(nationalId)
                                            .claimedPrizes(claimedPrizeList)
                                            .spinId(firstRecord.getId())
                                            .weekNumber(weekNumber)
                                            .build()
                            )
                    )
                    .build();

        } catch (Exception e) {
            logger.info(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                    .build();
        }
    }

    public Response spin(SpinReq spinReq, @Context ContainerRequestContext ctx) {
        String phoneNo = (String) ctx.getProperty("jwt.phone");
        String tokiId = (String) ctx.getProperty("jwt.tokiId");
        String nationalId = (String) ctx.getProperty("jwt.nationalId");
        Integer weekNumber = spinReq.getWeekNumber();

        if (phoneNo == null || tokiId == null || nationalId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                    .build();
        }

        if (!spinReq.getNationalId().equalsIgnoreCase(nationalId)){
            logger.info("National ID mismatch. Request: " + spinReq.getNationalId() + ", JWT Token: " + nationalId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                    .build();
        }

        boolean isBlackListed = helper.isBlacklisted(nationalId);

        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        AtomicInteger claimedPrizeId = new AtomicInteger();
        AtomicReference<String> coupon = new AtomicReference<>();
        AtomicReference<String> rechargedMsisdn = new AtomicReference<>();

        try {
            dsl.transaction(cfg -> {
                DSLContext dslTx = DSL.using(cfg);

                SpinEligibleNumbersRecord spinRecord = dslTx.selectFrom(SPIN_ELIGIBLE_NUMBERS)
                        .where(SPIN_ELIGIBLE_NUMBERS.NATIONAL_ID.eq(nationalId))
                        .and(SPIN_ELIGIBLE_NUMBERS.USED.eq(false))
                        .orderBy(SPIN_ELIGIBLE_NUMBERS.RECHARGE_DATE.asc())
                        .limit(1)
                        .forUpdate()
                        .skipLocked()
                        .fetchOne();

                if (spinRecord == null) {
                    logger.info("No remaining spins for " + nationalId);
                    throw new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(new CustomResponse<>("fail", "Танд хүрд эргүүлэх эрх байхгүй байна.", null))
                                    .build()
                    );
                }

                logger.info("Spin record: " + spinRecord.formatJSON());
                logger.info("Spin eligible ID from request: " + spinReq.getSpinId());
                logger.info("Spin eligible ID from record: " + spinRecord.getId());

                if (!spinReq.getSpinId().equals(spinRecord.getId())) {
                    logger.warn("Spin request has been rejected for " + nationalId + ". Reason: Spin ID mismatch. Request ID: " + spinReq.getSpinId() + ", Record ID: " + spinRecord.getId());
                    logger.warn("Exception will be thrown!");
                    throw new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                                    .build()
                    );
                }

                int updatedSpinRecord = dslTx.update(SPIN_ELIGIBLE_NUMBERS)
                        .set(SPIN_ELIGIBLE_NUMBERS.USED, true)
                        .where(SPIN_ELIGIBLE_NUMBERS.ID.eq(spinRecord.getId()))
                        .execute();

                logger.info("Updated Spin record: " + updatedSpinRecord);

                SpecialPrizeRuleRecord specialPrizeRuleRecord;

                rechargedMsisdn.set(spinRecord.getPhoneNo());

                if (!isBlackListed) {
                    specialPrizeRuleRecord = dslTx.selectFrom(SPECIAL_PRIZE_RULE)
                            .where(SPECIAL_PRIZE_RULE.CLAIMED.eq(false))
                            .and(SPECIAL_PRIZE_RULE.MATCH_DATE.eq(formattedDate))
                            .and(SPECIAL_PRIZE_RULE.MATCH_TIME.lessThan(formattedTime))
                            .orderBy(SPECIAL_PRIZE_RULE.MATCH_TIME.asc())
                            .limit(1)
                            .forUpdate()
                            .skipLocked()
                            .fetchOne();
                } else {
                    logger.info("Blacklisted national ID: " + nationalId + ", UUID: " + spinReq.getSpinId());
                    claimedPrizeId.set(0);
                    return;
                }

                logger.info("Special prize record: " + (specialPrizeRuleRecord != null ? specialPrizeRuleRecord.formatJSON() : "No prize available"));

                if (specialPrizeRuleRecord == null) {
                    claimedPrizeId.set(0);
                    return;
                }

                if (physicalPrizeIds.contains(specialPrizeRuleRecord.getPrizeId())) {
                    logger.info("Checking if the user " + nationalId + " has already won a super special prize before.");
                    SpecialPrizeRuleRecord existingSuperPrize = dslTx.selectFrom(SPECIAL_PRIZE_RULE)
                            .where(SPECIAL_PRIZE_RULE.CLAIMED.eq(true))
                            .and(SPECIAL_PRIZE_RULE.PRIZE_ID.in(physicalPrizeIds))
                            .and(SPECIAL_PRIZE_RULE.CLAIMED_USER_NATIONAL_ID.eq(nationalId))
                            .limit(1)
                            .fetchOne();

                    logger.info("Existing super prize record: " + (existingSuperPrize != null ? existingSuperPrize.formatJSON() : "No existing super prize"));

                    if (existingSuperPrize != null) {
                        logger.info("User " + nationalId + " has already won a super special prize before. Skipping super special prize.");
                        claimedPrizeId.set(0);
                        return;
                    }
                }

                if (couponPrizeIds.contains(specialPrizeRuleRecord.getPrizeId()))
                    coupon.set(specialPrizeRuleRecord.getCoupon());


                int updatedSpecialPrizeRecord = dslTx.update(SPECIAL_PRIZE_RULE)
                        .set(SPECIAL_PRIZE_RULE.CLAIMED, true)
                        .set(SPECIAL_PRIZE_RULE.CLAIMED_DATE, debugMode ? testNow : LocalDateTime.now())
                        .set(SPECIAL_PRIZE_RULE.CLAIMED_USER_NATIONAL_ID, nationalId)
                        .where(SPECIAL_PRIZE_RULE.ID.eq(specialPrizeRuleRecord.getId()))
                        .execute();

                claimedPrizeId.set(specialPrizeRuleRecord.getPrizeId());
                logger.info("Updated Special Prize record: " + updatedSpecialPrizeRecord);
            });
        } catch (Exception e) {
            logger.error("Error during spin transaction for national ID: " + nationalId, e);
            if (e instanceof WebApplicationException) {
                throw (WebApplicationException) e;
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new CustomResponse<>("fail", "Алдаа гарлаа. Дахин оролдоно уу.", null))
                    .build();
        }

        if (claimedPrizeId.get() != 0) {
            PrizeListRecord specialPrizeInfoRecord = dsl.selectFrom(PRIZE_LIST)
                    .where(PRIZE_LIST.ID.eq(claimedPrizeId.get()))
                    .limit(1)
                    .fetchOne();

            logger.info("Super prize info record: " + (specialPrizeInfoRecord != null ? specialPrizeInfoRecord.formatJSON() : "No prize info found"));

            if (couponPrizeIds.contains(claimedPrizeId.get())) {
                prizeService.processPrizeAsync(claimedPrizeId.get(), rechargedMsisdn.get(), nationalId, tokiId, spinReq.getSpinId(), coupon.get());
                return Response.ok()
                        .entity(new CustomResponse<>(
                                "success",
                                "Coupon Prize",
                                SpinRes.builder()
                                        .prizeId(specialPrizeInfoRecord.getId())
                                        .prizeName(specialPrizeInfoRecord.getPrizeName())
                                        .isSpecial(specialPrizeInfoRecord.getSpecial())
                                        .coupon(coupon.get())
                                        .build()
                        ))
                        .build();
            }

            if (physicalPrizeIds.contains(claimedPrizeId.get())) {
                prizeService.processPrizeAsync(claimedPrizeId.get(), rechargedMsisdn.get(), nationalId, tokiId, spinReq.getSpinId(), null);
                return Response.ok()
                        .entity(new CustomResponse<>(
                                "success",
                                "Physical Prize",
                                SpinRes.builder()
                                        .prizeId(specialPrizeInfoRecord.getId())
                                        .prizeName(specialPrizeInfoRecord.getPrizeName())
                                        .isSpecial(specialPrizeInfoRecord.getSpecial())
                                        .coupon(null)
                                        .build()
                        ))
                        .build();
            }

            return Response.ok().entity(
                    new CustomResponse<>(
                            "fail",
                            "Алдаа гарлаа. Дахин оролдоно уу.",
                            null
                    )
            ).build();


        } else {
            int roll = (int) (Math.random() * 100) + 1;
            logger.info("Regular price roll: " + roll);

            int regularPrizeId = 303;

            int[][] ranges = WEEK_RANGES.getOrDefault(
                    weekNumber,
                    new int[][]{{100, 303}}
            );

            for (int[] r : ranges) {
                if (roll <= r[0]) {
                    regularPrizeId = r[1];
                    break;
                }
            }

            prizeService.processPrizeAsync(regularPrizeId, rechargedMsisdn.get(), nationalId, tokiId, spinReq.getSpinId(), null);
            logger.info("Rolled regular prize id: " + regularPrizeId);

            PrizeListRecord regularGiftRecord = dsl.selectFrom(PRIZE_LIST)
                    .where(PRIZE_LIST.ID.eq(regularPrizeId))
                    .limit(1)
                    .fetchOne();

            return Response.ok()
                    .entity(new CustomResponse<>(
                                    "success",
                                    "Regular Prize",
                                    SpinRes.builder()
                                            .prizeId(regularGiftRecord.getId())
                                            .prizeName(regularGiftRecord.getPrizeName())
                                            .isSpecial(regularGiftRecord.getSpecial())
                                            .coupon(null)
                                            .build()
                            )
                    )
                    .build();
        }
    }
}