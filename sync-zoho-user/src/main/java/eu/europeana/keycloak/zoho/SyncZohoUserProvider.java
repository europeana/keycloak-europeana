package eu.europeana.keycloak.zoho;

import com.opencsv.bean.CsvToBeanBuilder;
import eu.europeana.api.common.zoho.ZohoConnect;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(SyncZohoUserProvider.class);

    private final KeycloakSession session;
    private final RealmModel      realm;
    private final UserProvider    userProvider;
    private final UserManager     userManager;
    private final ZohoConnect     zohoConnect = new ZohoConnect();

    private List<Account> accounts;
    private List<Contact> contacts;
    HashMap<String, Institute4Hash> instituteMap      = new HashMap<>();
    HashMap<String, String>         affiliatedUserMap = new HashMap<>();


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
    public String zohoSync(
        @DefaultValue("1") @QueryParam("days") int days) throws InterruptedException {
        LOG.info("ZohoSync called.");
        String accountsJob;
        String contactsJob;
        int numberOfUsersUpdatedInKeycloak=0;

        if (zohoConnect.getOrCreateAccessToZoho()) {
            ZohoBatchJob zohoBatchJob = new ZohoBatchJob();
            try {
                accountsJob = zohoBatchJob.ZohoBulkCreateJob("Accounts");
                contactsJob = zohoBatchJob.ZohoBulkCreateJob("Contacts");
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error creating bulk job.";
            }
            ZohoBatchDownload zohoBatchDownload = new ZohoBatchDownload();
            try {
                createAccounts(zohoBatchDownload.downloadResult(Long.valueOf(accountsJob)));
                createContacts(zohoBatchDownload.downloadResult(Long.valueOf(contactsJob)));
                if (accounts != null && !accounts.isEmpty() && contacts != null && !contacts.isEmpty()) {
                    synchroniseContacts(days);
                    numberOfUsersUpdatedInKeycloak =updateKCUsers();
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error downloading bulk job.";
            }
        }
        publishStatusReport(String.format("%s accounts in Zoho where compared against %s accounts in KeyCloak where the affiliation for %s accounts was changed or established.",affiliatedUserMap.size(),affiliatedUserMap.size(),numberOfUsersUpdatedInKeycloak));
        return "Done.";
    }

    private void publishStatusReport(String messsage) {
        //this.session.getKeycloakSessionFactory().publish(new SyncCompletionEvent());
        sendSlackHttpMessage(messsage);
    }



    private void sendSlackHttpMessage(String message) {
        try {

            CloseableHttpClient httpClient = HttpClients.createDefault();
            LOG.info(" SLACK WEBHOOK :  "+System.getenv("SLACK_WEBHOOK"));
            HttpPost httpPost = new HttpPost(System.getenv("SLACK_WEBHOOK"));
            LOG.info(" Sending message :" + message);
            StringEntity entity = new StringEntity(message);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
//            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
//                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                    LOG.info(" Successfully sent slack message ! "+ response.getStatusLine().getStatusCode());
//                }
//            } catch (Exception e) {
//                LOG.info("Error sending message in Slack! " +e.getMessage());
//            }
        }
        catch (Exception e){
            LOG.info("Exception occurred while sending slack http message ! " +e.getMessage());
        }

    }

    // skip the first line containing the CSV header
    private void createAccounts(String pathToAccountsCsv) throws Exception {
        accounts = new CsvToBeanBuilder(new FileReader(pathToAccountsCsv))
            .withType(Account.class)
            .withSkipLines(1)
            .build()
            .parse();
        Files.deleteIfExists(Paths.get(pathToAccountsCsv));
    }

    // skip the first line containing the CSV header
    private void createContacts(String pathToContactsCsv) throws Exception {
        contacts = new CsvToBeanBuilder(new FileReader(pathToContactsCsv))
            .withType(Contact.class)
            .withSkipLines(1)
            .build()
            .parse();
        Files.deleteIfExists(Paths.get(pathToContactsCsv));
    }

    private void synchroniseContacts(int days) {
        OffsetDateTime toThisTimeAgo = OffsetDateTime.now().minusDays(days);
        String         affiliation;
        for (Account account : accounts) {
            instituteMap.put(account.getID(),
                new Institute4Hash(account.getAccountName(), account.getEuropeanaOrgID()));
        }

        for (Contact contact : contacts) {
            String msg = null;
            affiliation = null;
            if (StringUtils.isNotBlank(contact.getAccountID()) && contact.getModifiedTime().isAfter(toThisTimeAgo)) {
                if (instituteMap.get(contact.getAccountID()) != null) {
                    affiliation = instituteMap.get(contact.getAccountID()).getEuropeanaOrgID();
                    if (StringUtils.isNotBlank(affiliation)) {
                        affiliatedUserMap.put(contact.getEmail(), affiliation);
                    }
                }
            }
        }
        LOG.info(affiliatedUserMap.size() + " contacts records with affiliated Europeana org.ID were updated in the past " + days + " days.");
    }

    private int updateKCUsers() {
        int updated = 0;
        LOG.info("Checking if updated contacts exist in Keycloak ...");
        for (Map.Entry<String, String> affiliatedUser : affiliatedUserMap.entrySet()) {

            if (userProvider.getUserByEmail(realm, affiliatedUser.getKey()) == null) {
            } else {
                UserModel user = userProvider.getUserByEmail(realm, affiliatedUser.getKey());
                user.setSingleAttribute("affiliation", affiliatedUser.getValue());
                updated ++;
                LOG.info(affiliatedUser.getKey() + " updated with affiliation " + affiliatedUser.getValue());
            }
        }
        LOG.info(updated + " users were found in Keycloak and had their affiliation updated.");
        return updated;
    }

    @Override
    public void close() {
    }

    public class Institute4Hash {

        private String accountName;
        private String europeanaOrgID;

        public Institute4Hash(String aName, String eID) {
            accountName    = aName;
            europeanaOrgID = eID;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getEuropeanaOrgID() {
            return europeanaOrgID;
        }
    }

}
