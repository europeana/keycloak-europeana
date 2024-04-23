package eu.europeana.keycloak.zoho;

import eu.europeana.api.common.zoho.ZohoConnect;

import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.utils.StringUtil;

/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(SyncZohoUserProvider.class);
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
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.userProvider = session.users();
        this.userManager = new UserManager(session);
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

//    @DefaultValue("1") @QueryParam("age") int minimumAgeInDays) {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String zohoSync(
            @DefaultValue("") @QueryParam("job") String job) {
        LOG.info("ZohoSync called.");
        String InstituteJobID;

        if (zohoConnect.getOrCreateAccessToZoho()) {
            if (StringUtil.isBlank(job)) {
                ZohoBulkJob zohoBulkJob = new ZohoBulkJob();
                try {
                    InstituteJobID = zohoBulkJob.ZohoBulkCreateJob("Accounts");
                    //                Thread.sleep(4000);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                    return "Zoho didn't like that at all.";
                }
                return "JobID: " + InstituteJobID;
            } else {
                try {
                    ZohoBulkDownload zohoBulkDownload = new ZohoBulkDownload();
                    zohoBulkDownload.downloadResult(Long.valueOf(job));
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                    return "Zoho is messing up. Again.";
                }
            }
            return "Done";
        }
        return "Could not connect to Zoho.";
    }


    /**
     * Retrieves users from Zoho
     *
     * @return String (completed message)
     */
//    @GET
//    @Produces({MediaType.APPLICATION_JSON})
//    public String zohoSync(
//        @DefaultValue("0") @QueryParam("from") int from,
//        @DefaultValue("0") @QueryParam("to") int to) {
//        LOG.info("ZohoSync called.");
//
//        int pages = from == 0 ? 1 : (to + 1) - from;
//
//        if (zohoConnect.getOrCreateAccessToZoho()){
//            ZohoInstitutes zohoInstitutes = new ZohoInstitutes();
//            try {
//                zohoInstitutes.getInstitutes(from, to);
//            } catch (Exception e) {
//                e.printStackTrace();
//                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
//                return "Zoho is messing up again.";
//            }
//
////            ZohoContacts zohoContacts = new ZohoContacts();
////            try {
////                lookupUserModel(zohoContacts.getContacts(from, to), pages);
////            } catch (Exception e) {
////                e.printStackTrace();
////                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
////                return "Grote grutt'n da's niet zo koel ofniedan! ";
////            }
//        }
//        return "Krek, da's allebarstes mooi zo.";
//    }
    private void lookupUserModel(Map<String, String> results, int pages) {
        int usersNotInKeycloak = 0;
        int usersInKeycloak = 0;
        int userInKCAndAffiliated = 0;
        int notAffiliated = 0;
        for (Map.Entry<String, String> contactAffiliation : results.entrySet()) {
            if (userProvider.getUserByEmail(realm, contactAffiliation.getKey()) == null) {
                LOG.info(contactAffiliation.getKey() + " NOT in KC");
                usersNotInKeycloak++;
            } else {
                usersInKeycloak++;
                if (StringUtil.isNotBlank(contactAffiliation.getValue())) {
                    LOG.info(contactAffiliation.getKey() + " in KC and affiliated with " + contactAffiliation.getValue());
                    userInKCAndAffiliated++;
                } else {
                    LOG.info(contactAffiliation.getKey() + " in KC but not affiliated");
                    notAffiliated++;
                }
            }

        }
        LOG.info("Summary: in " + pages + " pages, " + results.size() + " contacts with an Institute in Zoho: " + usersNotInKeycloak + " NOT in Keycloak "
                + usersInKeycloak + " in Keycloak; of those, " + userInKCAndAffiliated + " are affiliated, " + notAffiliated + "are not.");
    }

    @Override
    public void close() {
    }

}
