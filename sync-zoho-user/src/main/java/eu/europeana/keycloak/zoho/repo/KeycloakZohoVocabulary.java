package eu.europeana.keycloak.zoho.repo;

public final class KeycloakZohoVocabulary {

    private KeycloakZohoVocabulary(){
    }

    public static final String CLIENT_OWNER           = "client_owner";
    public static final String SHARED_OWNER           = "shared_owner";
    public static final String ACCOUNT_HOLDER         = "Account holder";
    public static final String API_USER               = "API User";
    public static final String API_CUSTOMER           = "API Customer";
    public static final boolean ENABLE_ZOHO_SYNC      = "true".equalsIgnoreCase(System.getenv("ZOHO_SYNC"));
    // Zoho batch constants
    public static final String CONTACTS               = "Contacts";
    public static final String ACCOUNTS               = "Accounts";
    public static final String API_PROJECTS           = "API_projects";

    //Comma separated values containing zoho contact ids for force update in zoho
    public static final String TEST_CONTACT_IDS_TO_UPDATE = "TEST_CONTACT_IDS_TO_UPDATE";

    //Comma separated values containing zoho project ids for force sync
    public static final String TEST_PROJECT_IDS_TO_UPDATE = "TEST_PROJECT_IDS_TO_UPDATE";

    public enum SyncScope {
        ALL,
        TEST_ONLY
    }
    public static final SyncScope SYNC_SCOPE;
    static {
        String envValue = System.getenv("SYNC_SCOPE");
        if (envValue != null && envValue.equalsIgnoreCase("ALL")) {
            //Only for production . Sync all eligible data from keycloak DB to zoho
            SYNC_SCOPE = SyncScope.ALL;
        } else {
            // Default to TEST_ONLY for safety
            SYNC_SCOPE = SyncScope.TEST_ONLY;
        }
    }

}