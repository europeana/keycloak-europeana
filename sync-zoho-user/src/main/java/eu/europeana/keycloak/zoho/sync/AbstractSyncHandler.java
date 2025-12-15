package eu.europeana.keycloak.zoho.sync;

import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import eu.europeana.keycloak.zoho.repo.CustomQueryRepository;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractSyncHandler {
    protected final RealmModel realm;
    protected final CustomQueryRepository repo;
    public AbstractSyncHandler(RealmModel realm, CustomQueryRepository repo) {
        this.realm = realm;
        this.repo = repo;
    }

    protected Map<String, KeycloakUser> userDetails = new HashMap<>();
    protected List<String> testUserIds = new ArrayList<>();

    /**
     * Initialize user Details map (holds the specific details for the KeycloakUser)
     * and the List of userIds belonging to test group.
     */
    public void loadKeycloakUsersFromDB(){
        userDetails =  repo.listAllUserMails(realm.getName());
        loadUserIdsBelongingToTestGroup();
    }

    /**
     * Initialize the list containing the keycloak user Ids intended only for test purposes.
     */
    private void loadUserIdsBelongingToTestGroup() {
        String testGroupId   =  repo.findTestGroupId();
        if(testGroupId!=null){
            testUserIds = repo.findTestGroupUsers(testGroupId);
        }
    }

    protected boolean isPartOfTestGroup(KeycloakUser keycloakUser) {
        return testUserIds.contains(keycloakUser.getId());
    }

}