package eu.europeana.keycloak.user;

/**
 * Created by luthien on 02/03/2022.
 */
public class UserRemovedEnvVariables {

    public static final String SLACK_WEBHOOK = System.getenv("SLACK_WEBHOOK");
    public static final String SLACK_USER    = System.getenv("SLACK_USER");


    static final String GRANT_TYPE      = "password";
    static final String CLIENT_ID       = System.getenv("CLIENT_ID");
    static final String CLIENT_SECRET   = System.getenv("CLIENT_SECRET");
    static final String SCOPE           = System.getenv("SCOPE");
    static final String DELETE_MGR_ID   = System.getenv("DELETE_MGR_ID");
    static final String DELETE_MGR_PW   = System.getenv("DELETE_MGR_PW");
    static final String SET_API_URL     = System.getenv("SET_API_URL");
    static final String AUTH_SERVER_URL = System.getenv("AUTH_SERVER_URL");
    static final String OIDTokenURL     = AUTH_SERVER_URL + "realms/europeana/protocol/openid-connect/token";


}
