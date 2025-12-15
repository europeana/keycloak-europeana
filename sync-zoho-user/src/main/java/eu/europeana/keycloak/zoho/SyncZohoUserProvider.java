package eu.europeana.keycloak.zoho;

import com.opencsv.bean.CsvToBeanBuilder;
import eu.europeana.api.common.zoho.ZohoConnect;
import eu.europeana.keycloak.SlackConnection;
import eu.europeana.keycloak.zoho.batch.ZohoBatchDownload;
import eu.europeana.keycloak.zoho.batch.ZohoBatchJob;
import eu.europeana.keycloak.zoho.datamodel.APIProject;
import eu.europeana.keycloak.zoho.datamodel.Account;
import eu.europeana.keycloak.zoho.datamodel.Contact;
import eu.europeana.keycloak.zoho.sync.ZohoContactSyncHandler;
import eu.europeana.keycloak.zoho.sync.ZohoProjectSyncHandler;
import eu.europeana.keycloak.zoho.repo.CustomQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.*;

/**
 * Created by luthien on 14/11/2022.
 */
@SuppressWarnings("javasecurity:S6096")
@Provider
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(SyncZohoUserProvider.class);
    public static final String SLACK_MESSAGE_REQUEST = """
            {"text":" %s "}
            """;
    private final RealmModel realm;
    private final UserProvider userProvider;
    private final CustomQueryRepository repo;

    private final ZohoConnect zohoConnect = new ZohoConnect();

    private List<Account> institutions;
    private List<Contact> contacts;
    private List<APIProject> apiProjects;

    public SyncZohoUserProvider(KeycloakSession session) {
        this.realm        = session.getContext().getRealm();
        this.userProvider = session.users();
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.repo = new CustomQueryRepository(entityManager);
    }

    @Override
    public Object getResource() {
        return this;
    }
    /**
     * Retrieves users from Zoho
     * @return String (completed message)
     */

    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String zohoSync(@DefaultValue("1") @QueryParam("days") int days) {

        LOG.info("ZohoSync called.  Contact sync - "+ENABLE_CONTACT_SYNC+" Project sync - "+ ENABLE_PROJECTS_SYNC);
        if (zohoConnect.getOrCreateAccessToZoho()) {
            try {
                loadModuleDataFromZoho();
            } catch (Exception e) {
                LOG.info("Message: " + e.getMessage() + "; cause: " + e);
                return "Error creating bulk job.";
            }
            //Synchronize zoho Contact Details (Zoho Contact entity represents User entity in keycloak )
            String contactSyncStatus = synchroniseContacts(days);
            //Synchronize API Projects (Zoho APIProject entity represents ProjectKey Client in keycloak )

            String projectSyncStatus = synchroniseAPIProjects();
            String status = contactSyncStatus + "         " +projectSyncStatus;
            //Publish Status Report For SYNC
            if(StringUtils.isNotEmpty(status)){
                SlackConnection conn = new SlackConnection("SLACK_WEBHOOK_API_AUTOMATION");
                conn.publishStatusReport(String.format(SLACK_MESSAGE_REQUEST,status));
            }
        }
        return "Done.";
    }

    private String synchroniseContacts(int days) {
        ZohoContactSyncHandler contactSync  = new ZohoContactSyncHandler(realm,repo,userProvider);
        return contactSync.syncContacts(days, institutions, contacts);
    }

    private String synchroniseAPIProjects() {
        ZohoProjectSyncHandler projectSync = new ZohoProjectSyncHandler(realm, repo);
        return projectSync.updateAPIProjects(apiProjects);
    }

    private void loadModuleDataFromZoho() throws Exception {
        //Register Async batch Job to download the details for required modules from ZOHO in csv form.
        ZohoBatchJob zohoBatchJob = new ZohoBatchJob();
        String accountsJob = zohoBatchJob.zohoBulkCreateJob(ACCOUNTS);
        String contactsJob = zohoBatchJob.zohoBulkCreateJob(CONTACTS);
        String apiProjectsJob = zohoBatchJob.zohoBulkCreateJob(API_PROJECTS);

        //Keep checking if jobs are finished ,download relevant CSV ones they are complete. Create data list from CSV.
        ZohoBatchDownload batch = new ZohoBatchDownload();
        if(StringUtils.isNotEmpty(accountsJob) && StringUtils.isNotEmpty(contactsJob)) {
            createAccounts(batch.downloadResult(Long.valueOf(accountsJob),ACCOUNTS));
            createContacts(batch.downloadResult(Long.valueOf(contactsJob),CONTACTS));
            LOG.info("Accounts and Contacts Loaded successfully!! ");
        }
        if(StringUtils.isNotEmpty(apiProjectsJob)) {
            createApiProjects(batch.downloadResult(Long.valueOf(apiProjectsJob),API_PROJECTS));
            LOG.info("Api Projects Loaded successfully!! ");
        }
    }

    private void createAccounts(String pathToAccountsCsv) throws IOException {
        // skip the first line containing the CSV header
        institutions = new CsvToBeanBuilder(new FileReader(pathToAccountsCsv))
            .withType(Account.class)
            .withSkipLines(1)
            .build()
            .parse();
        Files.deleteIfExists(Paths.get(pathToAccountsCsv));
    }

    private void createContacts(String pathToContactsCsv) throws IOException {
        // skip the first line containing the CSV header
        contacts = new CsvToBeanBuilder(new FileReader(pathToContactsCsv))
            .withType(Contact.class)
            .withSkipLines(1)
            .build()
            .parse();
        Files.deleteIfExists(Paths.get(pathToContactsCsv));
    }

    private void createApiProjects(String pathToContactsCsv) throws IOException {
        apiProjects = new CsvToBeanBuilder(new FileReader(pathToContactsCsv))
            .withType(APIProject.class)
            .withSkipLines(1)
            .build()
            .parse();
        Files.deleteIfExists(Paths.get(pathToContactsCsv));
    }

    @Override
    public void close() {
        // No action required
    }

}