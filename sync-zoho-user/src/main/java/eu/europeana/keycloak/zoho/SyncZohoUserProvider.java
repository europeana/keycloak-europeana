package eu.europeana.keycloak.zoho;

import eu.europeana.api.common.zoho.ZohoConnect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.utils.StringUtil;

/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG        = Logger.getLogger(SyncZohoUserProvider.class);
    private static final String LOG_PREFIX = "KEYCLOAK_EVENT:";
    private static final String SUCCESS_MSG = "Success";
    private static final String FAIL_MSG = "Failure";
    private static final String USERDEL_MSG = " were synchronised";

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
            ZohoContacts zohoContacts = new ZohoContacts();
            try {
                Map<String, String> contactAffiliations = zohoContacts.getContacts("debug");
                for (Map.Entry<String,String> contactAffiliation : contactAffiliations.entrySet()){
                    UserModel contactPeep = userProvider.getUserByEmail(realm, contactAffiliation.getKey());
                    if (contactPeep == null){
                        LOG.info("Too bad, no Keycloak user for email address " + contactAffiliation.getKey());
                    } else {
                        if (StringUtil.isNotBlank(contactAffiliation.getValue())){
                            LOG.info("Hey, " + getUserToUpdate(contactAffiliation.getKey()).getFirstName() + " is affiliated with " + contactAffiliation.getValue());
                        } else {
                            LOG.info("Too bad, " + getUserToUpdate(contactAffiliation.getKey()).getFirstName() + "'s institute does not have a Europeana org ID yet! ");
                        }
                    }
                }
                LOG.info("Done.");
                return "Done: fetched " + contactAffiliations.size() + " contacts";
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Grutjes. ";
            }
        }
        return "Ja, zeg dat wel. Grote grutt'n nog an toe!";
    }

    private UserModel getUserToUpdate(String email) {
        return userProvider.getUserByEmail(realm, email);
    }

    @Override
    public void close() {
    }

}
