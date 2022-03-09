package eu.europeana.keycloak.user;

/**
 * Created by luthien on 01/11/2021.
 */
public class UserRemovedMessage {


    public static final String LOG_PREFIX = "KEYCLOAK_EVENT:";

    static final String OK_ICON     = ":heavy_check_mark:";
    static final String ERROR_ICON  = ":x:";
    static final String OK_ASCII    = "✓";
    static final String ERROR_ASCII = "✘";

    static final String SLACK_USER_DELETE_MESSAGEBODY         =
        "{\"text\":\"On %s, user %s has requested to remove their account.\\n" +
        "This has just been done automatically for those systems marked with :heavy_check_mark: :\\n\\n" +
        "[%s] Keycloak\\n" +
        "[%s] The User Sets API\\n" +
        "[:x:] The recommendation engine\\n" +
        "[:x:] Mailchimp\\n\\n" +
        "From the remaining systems (marked with :x: above) their account " +
        "should be removed within 30 days (i.e. before %s).\"}";

    static final String SLACK_USER_DELETE_EMAILMESSAGE =
        "On %s, user %s has requested to remove their account.\n\n" +
        "This has just been done automatically for those systems marked with [✓]:\n\n" +
        "[%s] Keycloak\n" +
        "[%s] The User Sets Api\n" +
        "[✘] The recommendation engine\n" +
        "[✘] Mailchimp\n\n" +
        "From the remaining systems (marked with [✘] above) their account " +
        "should be removed within 30 days (i.e. before %s).\n\n\n" +
        "(PLEASE TAKE NOTE: this email was sent using a fail-over mechanism because the regular HTTP " +
        "request to this channel failed. Please check the logs and address the issue!)";

    static final String ACCESS_TOKEN                          = "User set access token: {}";
    static final String NO_ACCESS_TOKEN_FOUND                 = "No access token found in response from Keycloak: {}";
    static final String USERSET_DELETE_RESULT                 = "Usersets delete request was %ssuccesful: received HTTP %d response";

    static final String SENDING_CONFIRM_MSG_SLACK             = "Sending confirmation message to Slack";
    static final String SLACK_MSG_SENT                        = "Confirmation message was sent to Slack";

    static final String USERSET_TOKEN_POST_ERROR              = "Error sending POST request, received HTTP {} response";
    static final String USERSET_HTTP_IOEXCEPT                 = "IOException occurred sending Userset delete request by HTTP: ";
    static final String SLACK_MSG_UNSUPPORTED_EXCEPT          = "UnsupportedEncodingException occurred while creating Slack message";
    static final String SLACK_MSG_HTTP_IOEXCEPT               = "IOException occurred sending Slack HTTP message: ";
    static final String SLACK_MSG_EMAIl_EXCEPT                = "EmailException occurred while sending Slack message by email: ";
    static final String SLACK_MSG_RESULT_STATUS               = "Slack message %s sent: received HTTP %d response";

    static final String USERSET_DELETE_NO_SUCCESS             =
        "Usersets delete request was not successful: received HTTP {} " +
        "response.getStatusLine().getStatusCode() response. " +
        "Response body: {}";

    static final String HTTP_FAILED_TRYING_EMAIL              =
        "An error occurred trying to send the message over HTTP. Trying " +
        "to send it via email now ...";

    static final String HTTP_AND_EMAIL_FAILED                 =
        "!IMPORTANT! User account was removed, but failed to send " +
        "confirmation message to Slack. Please notify the Slack " +
        "delete_user_account channel, providing details about the error " +
        "and the deleted user, so they can handle the issue manually.";

    static final String SLACK_USER_NOT_FOUND                  = "Required Keycloak Slack user '%s' not found. Slack messages are sent on " +
                                                                "behalf of this user; please make sure a user with username '%s' exists.";

    static final String NO_SLACK_USER_EXITING                 = "Required Keycloak Slack user '%s' not found. Exiting ... ";


}
