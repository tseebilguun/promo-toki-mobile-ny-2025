package mn.unitel.campaign;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Helper {
    public boolean isTokiNumber(String msisdn) {
        return msisdn.matches("^(50[0-4]|55[0-4]).*");
    }
}
