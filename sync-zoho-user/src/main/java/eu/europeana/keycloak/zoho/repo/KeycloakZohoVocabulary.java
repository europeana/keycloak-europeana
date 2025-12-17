package eu.europeana.keycloak.zoho.repo;

public final class KeycloakZohoVocabulary {

    private KeycloakZohoVocabulary(){
    }

    public static final String CLIENT_OWNER           = "client_owner";
    public static final String SHARED_OWNER           = "shared_owner";
    public static final String ACCOUNT_HOLDER         = "Account holder";
    public static final String API_USER               = "API User";
    public static final String API_CUSTOMER           = "API Customer";
    public static final boolean ENABLE_CONTACT_SYNC   = "true".equalsIgnoreCase(System.getenv("ZOHO_CONTACT_SYNC"));
    public static final boolean ENABLE_PROJECTS_SYNC  = "true".equalsIgnoreCase(System.getenv("ZOHO_API_PROJECTS_SYNC"));
    // Zoho batch constants
    public static final String CONTACTS               = "Contacts";
    public static final String ACCOUNTS               = "Accounts";
    public static final String API_PROJECTS           = "API_projects";

    //Comma separated values containing zoho contact ids for force update in zoho
    public static final String TEST_CONTACT_IDS_TO_UPDATE = "TEST_CONTACT_IDS_TO_UPDATE";

    //Comma separated values containing zoho project ids for force sync
    public static final String TEST_PROJECT_IDS_TO_UPDATE = "TEST_PROJECT_IDS_TO_UPDATE";

}