package eu.europeana.api.common.zoho;

import static eu.europeana.api.common.zoho.GetRecords.getRecords;

import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.api.authenticator.store.TokenStore;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.dc.DataCenter.Environment;
import com.zoho.crm.api.dc.EUDataCenter;

import org.jboss.logging.Logger;
import static eu.europeana.api.common.zoho.ZohoAccessConfiguration.*;

/**
 * Main application
 *
 * @author Luthien Dulk
 * Created on 14 feb 2024
 */
public class ZohoConnect {

	private static final Logger LOG        = Logger.getLogger(ZohoConnect.class);
	private static final String LOG_PREFIX = "ZOHO_CONNECT:";

	// not clear if this is actually used. Delete if not.
	private final ZohoInMemoryTokenStore  tokenStore;

	public ZohoConnect(){
		this.tokenStore  = new ZohoInMemoryTokenStore();
	}


	public String ConnectToZoho() {
		LOG.info("Connecting to ZOHO");
		try {
			Environment environment = EUDataCenter.PRODUCTION;
			TokenStore  tokenStore  = new ZohoInMemoryTokenStore();

			LOG.info("ZOHO client ID: " + ZOHO_CLIENT_ID);
			LOG.info("ZOHO client secret: " + ZOHO_CLIENT_SECRET);
			LOG.info("ZOHO refresh token: " + ZOHO_REFRESH_TOKEN);
			LOG.info("ZOHO redirect URL: " + ZOHO_REDIRECT_URL);


			Token token = new OAuthToken
				.Builder()
				.clientID(ZOHO_CLIENT_ID)
				.clientSecret(ZOHO_CLIENT_SECRET)
				.refreshToken(ZOHO_REFRESH_TOKEN)
				.redirectURL(ZOHO_REDIRECT_URL)
				.build();

			LOG.info("Token built");

			new Initializer.Builder()
				.environment(environment)
				.token(token)
				.store(tokenStore)
				.initialize();

			LOG.info("Initializer built. Now calling Zoho:");

			// example usage, taken from Zoho's samples
			String moduleAPIName = "Leads";
			return getRecords(moduleAPIName);
		}
		catch (Exception e) {
			return e.toString();
		}
	}

}
