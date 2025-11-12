package mn.unitel.campaign;

import DTO.DTO_response.FetchServiceDetailsResponse;
import Executable.APIUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

@ApplicationScoped
public class ConsumerHandler {
    Logger logger = Logger.getLogger(ConsumerHandler.class);

    @Inject
    Helper helper;

    @Inject
    DSLContext dsl;

    @ConfigProperty(name = "campaign.debug.mode", defaultValue = "false")
    boolean debugMode;

    public void gotActive(String msisdn) {
        FetchServiceDetailsResponse servideDetails = APIUtil.fetchServiceDetails(msisdn, debugMode);
    }

    public void onRecharge(String msisdn) {

    }
}
