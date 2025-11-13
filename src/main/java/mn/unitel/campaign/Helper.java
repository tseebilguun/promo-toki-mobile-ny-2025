package mn.unitel.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

    private volatile Set<String> blacklistedNumbers = Collections.emptySet();

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

            blacklistedNumbers = cleaned;
            logger.infof("Loaded %d blacklisted numbers from %s", cleaned.size(), blacklistNumbersPath);
        } catch (Exception e) {
            logger.errorf(e, "Failed to load blacklist numbers from %s (continuing with empty blacklist)", blacklistNumbersPath);
        }
    }

    public boolean isTokiNumber(String msisdn) {
        return msisdn != null && msisdn.matches("^(50[0-4]|55[0-4]).*");
    }

    public boolean isBlacklisted(String msisdn) {
        if (msisdn == null) {
            return false;
        }
        return blacklistedNumbers.contains(msisdn);
    }
}