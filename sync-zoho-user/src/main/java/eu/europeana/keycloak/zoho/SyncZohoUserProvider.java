package eu.europeana.keycloak.zoho;

import com.opencsv.bean.CsvToBeanBuilder;
import eu.europeana.api.common.zoho.ZohoConnect;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
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

    private static final Logger LOG         = Logger.getLogger(SyncZohoUserProvider.class);

    private final KeycloakSession session;
    private final RealmModel      realm;
    private final UserProvider    userProvider;
    private final UserManager     userManager;
    private final ZohoConnect     zohoConnect = new ZohoConnect();

    private List<Account> accounts;
    private List<Contact> contacts;
    HashMap<String, Institute4Hash> instituteMap = new HashMap<>();


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
        @DefaultValue("1") @QueryParam("days") int lastChangeDaysAgo) throws InterruptedException {
        LOG.info("ZohoSync called.");
        String accountsJob;
        String contactsJob;

        if (zohoConnect.getOrCreateAccessToZoho()) {
            ZohoBulkJob zohoBulkJob = new ZohoBulkJob();
            try {
                accountsJob = zohoBulkJob.ZohoBulkCreateJob("Accounts");
                contactsJob = zohoBulkJob.ZohoBulkCreateJob("Contacts");
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error creating bulk job.";
            }
            ZohoBulkDownload zohoBulkDownload = new ZohoBulkDownload();
            try {
                createAccounts(zohoBulkDownload.downloadResult(Long.valueOf(accountsJob)));
                createContacts(zohoBulkDownload.downloadResult(Long.valueOf(contactsJob)));
                if (accounts != null && !accounts.isEmpty() && contacts != null && !contacts.isEmpty()) {
                    synchroniseContacts();
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.info("Message: " + e.getMessage() + "; cause: " + e.getCause());
                return "Error downloading bulk job.";
            }
        }
        return "Done.";
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

    private void synchroniseContacts() {
        for (Account account : accounts) {
            instituteMap.put(account.getID(),
                             new Institute4Hash(account.getAccountName(), account.getEuropeanaOrgID()));
        }

        for (Contact contact : contacts) {
            if (StringUtils.isNotBlank(contact.getAccountID())) {
                if (instituteMap.get(contact.getAccountID()) != null) {
                    System.out.println(contact.getEmail() + " affiliated with " +
                                       instituteMap.get(contact.getAccountID()).getAccountName() + "\n" +
                                       instituteMap.get(contact.getAccountID()).getEuropeanaOrgID());
                }
            }

        }
    }

    private void lookupUserModel(Map<String, String> results, int pages) {
        int usersNotInKeycloak    = 0;
        int usersInKeycloak       = 0;
        int userInKCAndAffiliated = 0;
        int notAffiliated         = 0;
        for (Entry<String, String> contactAffiliation : results.entrySet()) {
            if (userProvider.getUserByEmail(realm, contactAffiliation.getKey()) == null) {
                LOG.info(contactAffiliation.getKey() + " NOT in KC");
                usersNotInKeycloak++;
            } else {
                usersInKeycloak++;
                if (StringUtil.isNotBlank(contactAffiliation.getValue())) {
                    LOG.info(
                        contactAffiliation.getKey() + " in KC and affiliated with " + contactAffiliation.getValue());
                    userInKCAndAffiliated++;
                } else {
                    LOG.info(contactAffiliation.getKey() + " in KC but not affiliated");
                    notAffiliated++;
                }
            }

        }
        LOG.info("Summary: in " + pages + " pages, " + results.size() + " contacts with an Institute in Zoho: " +
                 usersNotInKeycloak + " NOT in Keycloak "
                 + usersInKeycloak + " in Keycloak; of those, " + userInKCAndAffiliated + " are affiliated, " +
                 notAffiliated + "are not.");
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
