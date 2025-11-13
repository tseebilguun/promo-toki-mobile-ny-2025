package mn.unitel.campaign;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Helper {
    Logger logger = Logger.getLogger(Helper.class.getName());

    @ConfigProperty(name = "blacklist.numbers.path", defaultValue = "config/blacklist-numbers.json")
    String blacklistNumbersPath;

    public boolean isTokiNumber(String msisdn) {
        return msisdn.matches("^(50[0-4]|55[0-4]).*");
    }
}
