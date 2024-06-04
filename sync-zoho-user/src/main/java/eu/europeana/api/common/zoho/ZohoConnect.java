package eu.europeana.api.common.zoho;

import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.ZOHO_CLIENT_ID;
import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.ZOHO_CLIENT_SECRET;
import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.ZOHO_REDIRECT_URL;
import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.ZOHO_REFRESH_TOKEN;
import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.ZOHO_USER_NAME;

import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.api.logger.Logger.Levels;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.SDKConfig;
import com.zoho.crm.api.UserSignature;
import com.zoho.crm.api.dc.DataCenter.Environment;
import com.zoho.crm.api.dc.EUDataCenter;
import org.jboss.logging.Logger;

/**
 * Class with connection to Zoho code
 *
 * @author Luthien Dulk
 * Created on 14 feb 2024
 */
public class ZohoConnect {

    private static final Logger LOG = Logger.getLogger(ZohoConnect.class);
    private static final Environment ENVIRONMENT = EUDataCenter.PRODUCTION;

    private ZohoInMemoryTokenStore tokenStore;

    public ZohoConnect() {
        // empty constructor
    }

    /**
     * Initialises the Zoho connection
     * @return boolean indicating success
     * @throws RuntimeException if the setup failed
     */
    public boolean initialise() throws RuntimeException {
        try {

            com.zoho.api.logger.Logger zlogger = new com.zoho.api.logger.Logger.Builder()
                    .level(Levels.INFO)
                    .filePath("//opt//keycloak//java_sdk_log.log")
                    .build();

            UserSignature userSignature = new UserSignature(ZOHO_USER_NAME);
            LOG.info("Connecting to ZOHO");

            LOG.info("ZOHO client ID: " + ZOHO_CLIENT_ID);
            LOG.info("ZOHO client secret: " + ZOHO_CLIENT_SECRET);
            LOG.info("ZOHO refresh token: " + ZOHO_REFRESH_TOKEN);
            LOG.info("ZOHO redirect URL: " + ZOHO_REDIRECT_URL);


            Token token = new OAuthToken.Builder()
                    .userSignature(userSignature)
                    .clientID(ZOHO_CLIENT_ID)
                    .clientSecret(ZOHO_CLIENT_SECRET)
                    .refreshToken(ZOHO_REFRESH_TOKEN)
                    .redirectURL(ZOHO_REDIRECT_URL)
                    .build();

            LOG.info("Token built");

            SDKConfig sdkConfig = new SDKConfig.Builder()
                    .autoRefreshFields(false)
                    .pickListValidation(true)
                    .build();

            LOG.info("SDK configured ...");


            new Initializer.Builder()
                    .environment(ENVIRONMENT)
                    .token(token)
                    .store(tokenStore)
                    .SDKConfig(sdkConfig)
                    .logger(zlogger)
                    .initialize();


            LOG.info("... and initialised.");

            return true;

        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean getOrCreateAccessToZoho() {
        if (tokenStore != null) {
            Token tokenById = tokenStore.findTokenById(ZOHO_USER_NAME);
            if (tokenById instanceof OAuthToken) {
                LOG.info("Token Expires in : -" + ((OAuthToken) tokenById).getExpiresIn());
            }

        } else {
            tokenStore = new ZohoInMemoryTokenStore();
        }
        return this.initialise();
    }
}
