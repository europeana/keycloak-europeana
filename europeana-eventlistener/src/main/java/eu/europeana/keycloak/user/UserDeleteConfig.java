package eu.europeana.keycloak.user;

/**
 * Created by luthien on 01/11/2021.
 */
public class UserDeleteConfig {

    public static final String JSONLOGPREFIXENVVAR = "KEYCLOAK_JSONLOG_PREFIX";
    public static final String LOG_PREFIX          = "KEYCLOAK_EVENT:";

    public static final String SLACK_WEBHOOK = "SLACK_WEBHOOK";
    public static final String SLACK_USER    = "SLACK_USER";

//    public static final boolean DEBUG = true;
    public static final boolean DEBUG = false;

    public static final String SET_API_URL = "https://set-api-test.eanadev.org/set/";


    public static final  String ERROR_ICON                            = ":x:";
    public static final  String ERROR_ASCII                           = "✘";
    public static final  String OK_ICON                               = ":heavy_check_mark:";
    public static final  String OK_ASCII                              = "✓";
    public static final  String SLACK_USER_DELETE_MESSAGEBODY         =
        "{\"text\":\"On %s, user %s has requested to remove their account.\\n" +
        "This has just been done automatically for those systems marked with :heavy_check_mark: :\\n\\n" +
        "[%s] Keycloak\\n" + "[%s] The User Sets API\\n" + "[:x:] The recommendation engine\\n" +
        "[:x:] Mailchimp\\n\\n" +
        "From the remaining systems (marked with :x: above) their account should be removed within 30 days (before %s).\"}";
    private static final String REQUEST_RECEIVED                      = "A problem prevented sending a Delete User Account notification for user ID %s to Slack.";
    private static final String NO_ACTION_BUT_LOGGED                  = "carrying out this request.\\nNo action was taken.\\nThe user token has been logged in Kibana.";
    private static final String NO_ACTION_BUT_LOGGED_PERIOD           = NO_ACTION_BUT_LOGGED + "\"}";

    public static final  String SLACK_SERVICE_UNAVAILABLE_MESSAGEBODY =
        REQUEST_RECEIVED + "a server error occurred which prevented " + NO_ACTION_BUT_LOGGED_PERIOD;
    private static final String NO_ACTION_LOGGED_AND_ERROR            = NO_ACTION_BUT_LOGGED + "\\n\\n[%s]\"}";
    public static final  String SLACK_FORBIDDEN_MESSAGEBODY           =
        REQUEST_RECEIVED + "an authorisation/authentication problem for the embedded Keycloak User prevented " +
        NO_ACTION_LOGGED_AND_ERROR;

}
