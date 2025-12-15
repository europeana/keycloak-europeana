package eu.europeana.keycloak.zoho.sync;

import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.Record;
import eu.europeana.keycloak.zoho.datamodel.APIProject;
import eu.europeana.keycloak.zoho.datamodel.KeycloakClient;
import eu.europeana.keycloak.zoho.repo.CustomQueryRepository;
import eu.europeana.keycloak.zoho.util.SyncHelper;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.ENABLE_PROJECTS_SYNC;
import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.TEST_PROJECT_IDS_TO_UPDATE;

/**
 * Service to handle the updates in API_Projects module of zoho, based on keycloak clients representing project keys
 */
public class ZohoProjectSyncHandler extends AbstractSyncHandler{
    private static final Logger LOG = Logger.getLogger(ZohoProjectSyncHandler.class);
    public static final String PROJECT_SYNC_MESSAGE = " %s Projects are updated in zoho.";

    private  final SyncHelper utils;
    private final List<String> preconfiguredProjectIdsToSync;
    public ZohoProjectSyncHandler(RealmModel realm, CustomQueryRepository repo) {
        super(realm,repo);
        this.utils = new SyncHelper();
        preconfiguredProjectIdsToSync = utils.tokenizeString(System.getenv(TEST_PROJECT_IDS_TO_UPDATE),",");
    }

    /**
     * Compares the keycloak projectClients and zoho projects  and
     * calls zoho for updating project details such as 'lastAccessDate'
     * @param apiProjects  List of Objects containing records from zoho 'API_Projects' module.
     * @return status
     */
    public String  updateAPIProjects(List<APIProject> apiProjects) {
        try {
            Map<Long, KeycloakClient> apiProjectsToUpdate = getApiProjectsToUpdate(apiProjects);

            ZohoUpdater updater = new ZohoUpdater();
            updater.updateInBatches(generateRequestObject(apiProjectsToUpdate), "API_projects");
            return (String.format(PROJECT_SYNC_MESSAGE,apiProjectsToUpdate.size()));

        } catch (SDKException e) {
            LOG.error("Error occurred while updating API_Project: " + e.getMessage());
            return "";
        }
    }


    /**
     * Generates map of zohoID and respective KeycloakClient object
     * whose details will be updated in zoho.
     *
     * @param apiProjects list of zohoProjects
     * @return map with id and client object
     */

    private Map<Long, KeycloakClient> getApiProjectsToUpdate(List<APIProject> apiProjects) {

        Map<String, KeycloakClient> projectClients = repo.getProjectClients(realm.getName());
        Map<Long,KeycloakClient> apiProjectsToUpdate = new HashMap<>();

        for (APIProject zohoProject : apiProjects) {
            KeycloakClient client = projectClients.get(zohoProject.getKey());
            if(isZohoUpdateCallRequired(zohoProject, client)){
                apiProjectsToUpdate.put(Long.parseLong(zohoProject.getId()), client);
            }
        }
        LOG.info("Project Ids to be updated in ZOHO -"+ apiProjectsToUpdate.entrySet());
        return apiProjectsToUpdate;
    }

    /**
     * Check if Zoho API_Project update is required.
     *
     * <p>The changed details are synced if the global switch is ON,</p>
     * <p>With exception for preconfigured project IDs which are updated
     * even if global sync switch 'ZOHO_API_PROJECTS_SYNC'is OFF.</p>
     */
    private boolean isZohoUpdateCallRequired(APIProject zohoProject, KeycloakClient client) {
        if (client == null || !isToUpdateZohoProject(zohoProject, client)) {
            return false;
        }
        // Condition is  : Sync is ON  or  (Sync is OFF and project is pre-configured)
        return ENABLE_PROJECTS_SYNC || isPreconfiguredForUpdate(zohoProject);
    }

    private  boolean isToUpdateZohoProject(APIProject zohoProject, KeycloakClient client) {
        LOG.info("Comparing last access dates for client " + client.getKey() + " keycloak - "
                + client.getLastAccessDate() + ", zoho -" + zohoProject.getLastAccess());

        // if the last access date value is changed , then consider it for updating in zoho
        return utils.isDateChangedInSource(zohoProject.getLastAccess(), client.getLastAccessDate());
    }
    private List<Record> generateRequestObject(Map<Long, KeycloakClient> apiProjectsToUpdate) {
        List<Record> records = new ArrayList<>();
        for(Map.Entry<Long,KeycloakClient> entry : apiProjectsToUpdate.entrySet()){
            //prepare record to update and add it in list
            Record recordToUpdate = new Record();
            recordToUpdate.setId(entry.getKey());
            recordToUpdate.addKeyValue("Last_access", OffsetDateTime.parse(entry.getValue().getLastAccessDate()));
            records.add(recordToUpdate);
        }
        return records;
    }

    private boolean isPreconfiguredForUpdate(APIProject project) {
        if(preconfiguredProjectIdsToSync.contains(project.getId())) {
            LOG.info("Found Project " + project.getKey() + " - " + project.getId() + " preconfigured to be updated in zoho !");
            return true;
        }
        return false;
    }
}