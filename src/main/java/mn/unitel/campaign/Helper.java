package mn.unitel.campaign;

import Executable.APIUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class Helper {

    private static final Logger logger = Logger.getLogger(Helper.class);

    @ConfigProperty(name = "blacklist.numbers.path", defaultValue = "config/blacklist-numbers.json")
    String blacklistNumbersPath;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty (name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    private volatile Set<String> blackListedNationalIds = Collections.emptySet();

    @ConfigProperty (name = "campaign.test.numbers", defaultValue = "")
    List<String> testNumbers;

    @PostConstruct
    void init() {
        loadBlacklist();
    }

    private void loadBlacklist() {
        File file = new File(blacklistNumbersPath);
        if (!file.exists()) {
            logger.warnf("Blacklist file not found at path: %s (continuing with empty blacklist)", blacklistNumbersPath);
            return;
        }

        try (InputStream is = new FileInputStream(file)) {
            List<String> list = objectMapper.readValue(is, new TypeReference<>() {});
            Set<String> cleaned = list.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());

            blackListedNationalIds = cleaned;
            logger.infof("Loaded %d blacklisted national IDs from %s", cleaned.size(), blacklistNumbersPath);
        } catch (Exception e) {
            logger.errorf(e, "Failed to load blacklist national IDs from %s (continuing with empty blacklist)", blacklistNumbersPath);
        }
    }

    public boolean isTokiNumber(String msisdn) {
        return msisdn != null && (msisdn.matches("^551[2-9][0-9]{4}$") || testNumbers.contains(msisdn));
    }

    public boolean isBlacklisted(String nationalId) {
        if (nationalId == null) {
            return false;
        }
        return blackListedNationalIds.contains(nationalId);
    }

    public Integer getCurrentWeekNumber(LocalDateTime now) {
        LocalDate date = now.toLocalDate();

        if (date.getYear() != 2025 || date.getMonth() != Month.DECEMBER) {
            return 0;
        }

        LocalDate dec1 = LocalDate.of(2025, 12, 1);
        int dayOffset = (int) ChronoUnit.DAYS.between(dec1, date);

        int week = (dayOffset / 7) + 1;

        return Math.min(week, 5);
    }

    public String getNationalIdByPhoneNo(String phoneNo, String accountName) {
        String rd;
        try {
            JsonNode rdInfo = Utils.toJsonNode(APIUtil.getRegIDByPhoneNo(phoneNo, accountName, debugMode));
            rd = rdInfo.path("rd").asText();
        } catch (Exception e) {
            return "NOT_FOUND";
        }

        return rd;
    }
}