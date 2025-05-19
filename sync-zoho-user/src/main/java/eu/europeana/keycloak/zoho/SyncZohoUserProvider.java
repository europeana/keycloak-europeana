package eu.europeana.keycloak.zoho;

import com.opencsv.bean.CsvToBeanBuilder;
import eu.europeana.api.common.zoho.ZohoConnect;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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

@Provider
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(SyncZohoUserProvider.class);

     public static final String SYNC_REPORT_STATUS_MESSAGE = "{\"text\":\" %s accounts in Zoho where compared against %s accounts in KeyCloak where:  %s accounts are shared and %s contacts were added to Zoho. \n"
        + "\n"
        + "The affiliation for %s accounts was changed or established.\"}";

    private final KeycloakSession session;
    private final RealmModel      realm;
    private final UserProvider    userProvider;
    private final UserManager     userManager;
    private final ZohoConnect     zohoConnect = new ZohoConnect();

    private List<Account> accounts;
    private List<Contact> contacts;
    HashMap<String, Institute4Hash> instituteMap    = new HashMap<>();
    HashMap<String, String>         modifiedUserMap = new HashMap<>();


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


    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String zohoSync(@DefaultValue("1") @QueryParam("days") int days) {
        LOG.info("ZohoSync called.");
        String accountsJob;
        String contactsJob;
        int    nrUpdatedUsers = 0;
        int nrOfNewlyAddedContactsInZoho =0;

        if (zohoConnect.getOrCreateAccessToZoho()) {
            ZohoBatchJob zohoBatchJob = new ZohoBatchJob();
            try {
                accountsJob = zohoBatchJob.ZohoBulkCreateJob("Accounts");
                contactsJob = zohoBatchJob.ZohoBulkCreateJob("Contacts");
            } catch (Exception e) {
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error creating bulk job.";
            }
            ZohoBatchDownload zohoBatchDownload = new ZohoBatchDownload();
            try {
                createAccounts(zohoBatchDownload.downloadResult(Long.valueOf(accountsJob)));
                createContacts(zohoBatchDownload.downloadResult(Long.valueOf(contactsJob)));
                if (accounts != null && !accounts.isEmpty() && contacts != null && !contacts.isEmpty()) {
                    synchroniseContacts(days);
                    nrOfNewlyAddedContactsInZoho = createNewZohoContacts(contacts);
                    nrUpdatedUsers = updateKCUsers();
                }
            } catch (Exception e) {
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error downloading bulk job.";
            }
        }
        publishStatusReport(generateStatusReport(nrUpdatedUsers,nrOfNewlyAddedContactsInZoho));
        return "Done.";
    }

    private int createNewZohoContacts(List<Contact> contacts) {
        KeycloakToZohoSyncService kzSync = new KeycloakToZohoSyncService(session);
        int contactsCreated= kzSync.validateAndCreateZohoContact(contacts);
        return contactsCreated;
    }

    private String generateStatusReport(int nrUpdatedUsers,int nrOfNewlyAddedContactsInZoho) {
        return String.format(SYNC_REPORT_STATUS_MESSAGE,
                             contacts.size(),
                             userProvider.getUsersCount(realm),
                             modifiedUserMap.size(),
                             nrOfNewlyAddedContactsInZoho,
                             nrUpdatedUsers);
    }

    private void publishStatusReport(String message) {
        LOG.info("Sending Slack Message : " + message);
        try {
            String slackWebhookApiAutomation = System.getenv("SLACK_WEBHOOK_API_AUTOMATION");
            if (StringUtils.isBlank(slackWebhookApiAutomation)) {
                LOG.error("Slack webhook not configured, status report will not be published over Slack.");
                return;
            }
            HttpPost     httpPost = new HttpPost(slackWebhookApiAutomation);
            StringEntity entity   = new StringEntity(message);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {
                LOG.info("Received status " + response.getStatusLine().getStatusCode()
                         + " while calling slack!");
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    LOG.info(" Successfully sent slack message !");
                }
            }
        } catch (IOException e) {
            LOG.error("Exception occurred while sending slack message !! " + e.getMessage());
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
        for (Account account : accounts) {
            instituteMap.put(account.getID(),
                             new Institute4Hash(account.getAccountName(), account.getEuropeanaOrgID()));
        }
        zohoConnect.getOrCreateAccessToZoho();
        for (Contact contact : contacts) {
            calculateModifiedZohoUsers(contact, toThisTimeAgo);
            handleZohoUpdate(contact);
        }
        LOG.info(
            modifiedUserMap.size() + " contacts records were updated in Zoho in the past " + days + " days.");
    }

    private void handleZohoUpdate(Contact contact) {
        KeycloakToZohoSyncService kzSync = new KeycloakToZohoSyncService(session);
        Map<String,String> updatedContacts = new HashMap<>();
        updatedContacts = kzSync.handleZohoUpdate(contact);
    }

    private void calculateModifiedZohoUsers(Contact contact, OffsetDateTime toThisTimeAgo) {
        if (contact.getModifiedTime().isAfter(toThisTimeAgo)) {
            //When zoho Modified Contact is associated to the Organization
            if (StringUtils.isNotBlank(contact.getAccountID()) &&
                instituteMap.get(contact.getAccountID()) != null) {
                modifiedUserMap.put(contact.getEmail(),
                                    instituteMap.get(contact.getAccountID()).getEuropeanaOrgID());
            }
            //When zoho Modified contact is  not associated to organization anymore
            else if (StringUtils.isBlank(contact.getAccountID())) {
                modifiedUserMap.put(contact.getEmail(), null);
            }
        }
    }

    private int updateKCUsers() {
        int updated = 0;
        LOG.info("Checking if updated contacts exist in Keycloak ...");
        for (Map.Entry<String, String> affiliatedUser : modifiedUserMap.entrySet()) {
            UserModel user = userProvider.getUserByEmail(realm, affiliatedUser.getKey());
            if (user != null) {
                String zohoOrgId = affiliatedUser.getValue();
                //In case zoho orgID does not match with existing affiliation in keycloak then update the keycloak affiliation
                String affiliationValue = user.getFirstAttribute("affiliation");
                boolean isToUpdateAffiliation = (StringUtils.isNotBlank(zohoOrgId) ? !zohoOrgId.equals(
                    affiliationValue) : StringUtils.isNotBlank(affiliationValue));

                if (isToUpdateAffiliation) {
                    user.setSingleAttribute("affiliation", zohoOrgId);
                    updated++;
                    LOG.info(affiliatedUser.getKey() + " affiliation updated from : " + affiliationValue + " to " +
                             zohoOrgId);
                } else {
                    LOG.info(affiliatedUser.getKey() + " will not be updated");
                }

            }
        }
        LOG.info(updated + " users were found in Keycloak and had their affiliation updated.");
        return updated;
    }

    @Override
    public void close() {
        // No action required
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