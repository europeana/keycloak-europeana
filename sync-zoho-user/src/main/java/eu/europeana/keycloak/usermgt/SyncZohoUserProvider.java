package eu.europeana.keycloak.usermgt;

import static eu.europeana.api.common.zoho.GetRecords.getRecords;
import static org.keycloak.utils.StringUtil.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;
import eu.europeana.api.common.zoho.*;

/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG        = Logger.getLogger(SyncZohoUserProvider.class);
    private static final String LOG_PREFIX = "KEYCLOAK_EVENT:";
    private static final String SUCCESS_MSG = "Success";
    private static final String FAIL_MSG = "Failure";
    private static final String USERDEL_MSG = " were synchronised";

    private static Map<String, String> EMAIL_NOT_VERIFIED;



    private KeycloakSession session;

    private RealmModel realm;

    private UserProvider userProvider;

    private UserManager userManager;

    private ZohoConnect zohoConnect = new ZohoConnect();

    public SyncZohoUserProvider(KeycloakSession session) {
        this.session      = session;
        this.realm        = session.getContext().getRealm();
        this.userProvider = session.users();
        this.userManager  = new UserManager(session);

    }

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Retrieves users from Zoho
     *
     * @return String (completed message)
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String zohoSync() {
        LOG.info("ZohoSync called.");
        if (zohoConnect.getOrCreateAccessToZoho()){
            // example usage, taken from Zoho's samples
            try {
                return getRecords("Leads");
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Grutjes. ";
            }
        }
        return "Ja, zeg dat wel. Grote grutt'n nog an toe!";
    }

    @Override
    public void close() {
    }

}
