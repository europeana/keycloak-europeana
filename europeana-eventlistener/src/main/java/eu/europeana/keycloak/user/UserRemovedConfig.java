package eu.europeana.keycloak.user;

/**
 * Created by luthien on 01/11/2021.
 */
public class UserRemovedConfig {
    public static final String LOG_PREFIX = "[KEYCLOAK_EVENT] ";

    protected static final String ERROR_ICON = ":x:";

    static final boolean DEBUG_LOGS = System.getenv("DEBUG_LOGS").equalsIgnoreCase("true");
    static final String GRANT_TYPE = "password";
    static final String CLIENT_ID = System.getenv("CLIENT_ID");
    static final String CLIENT_SECRET = System.getenv("CLIENT_SECRET");
    static final String SCOPE = System.getenv("SCOPE");
    static final String DELETE_MGR_ID = System.getenv("DELETE_MGR_ID");
    static final String DELETE_MGR_PW = System.getenv("DELETE_MGR_PW");
    static final String SET_API_URL = System.getenv("SET_API_URL");
    static final String AUTH_SERVER_URL = System.getenv("AUTH_SERVER_URL");
    static final String SLACK_WEBHOOK = System.getenv("SLACK_WEBHOOK");
    static final String SLACK_USERNAME = System.getenv("SLACK_USER");
    static final String OIDTokenURL = AUTH_SERVER_URL + "/realms/europeana/protocol/openid-connect/token";

    static final String ERROR_ASCII = "✘";
    static final String OK_ICON = ":heavy_check_mark:";
    static final String OK_ASCII = "✓";
    static final String SLACK_USER_DELETE_MESSAGEBODY;
    static final String MSG_PROJECT_KEY_WITH_NO_USER = "\\n Project key %s has no user associated to it.";
    static {
        SLACK_USER_DELETE_MESSAGEBODY =
            "{\"text\":\"On %s, user %s has requested to remove their " +
                    "account.\\nThis has just been done automatically for those " +
                    "systems marked with :heavy_check_mark: :\\n\\n[%s] " +
                    "Keycloak\\n[%s] The User Sets API\\n" + "[:x:] The " +
                    "recommendation engine\\n[:x:] Mailchimp\\n\\nFrom the " +
                    "remaining systems (marked with :x: above) their account " +
                "should be removed within 30 days (before %s).%s\"}";
    }

    static final String REQUEST_RECEIVED = "A problem prevented sending a Delete User Account " +
            "notification for user ID %s to Slack.";
    static final String NO_ACTION_BUT_LOGGED = "carrying out this request.\\nNo action was taken.\\nThe " +
            "user token has been logged in Kibana.";
    static final String NO_ACTION_BUT_LOGGED_PERIOD = NO_ACTION_BUT_LOGGED + "\"}";

    static final String SLACK_SERVICE_UNAVAILABLE_MESSAGEBODY = REQUEST_RECEIVED +
            "a server error occurred which prevented " +
            NO_ACTION_BUT_LOGGED_PERIOD;
    static final String NO_ACTION_LOGGED_AND_ERROR = NO_ACTION_BUT_LOGGED + "\\n\\n[%s]\"}";
    static final String SLACK_FORBIDDEN_MESSAGEBODY = REQUEST_RECEIVED +
            "an authorisation/authentication problem for the " +
            "embedded Keycloak User prevented " +
            NO_ACTION_LOGGED_AND_ERROR;

    static final String USER_SETS_DELETED = "User sets deleted.";
    static final String USER_SETS_NOT_DELETED = "No user sets deleted.";
    static final String SENDING_CONFIRM_MSG_SLACK = "Sending confirmation message to Slack";
    static final String HTTP_FAILED_TRYING_EMAIL = "Error occurred trying to send the message over HTTP, now trying " +
            "to send it via email ...";
    static final String HTTP_AND_EMAIL_FAILED = "!IMPORTANT! User account was removed, but failed to send " +
            "confirmation message to Slack. Please notify the Slack " +
            "delete_user_account channel, providing details about the error " +
            "and the deleted user, so they can handle the issue manually.";

    static final String SLACK_MSG_SENT = "Confirmation message was sent to Slack";

}
