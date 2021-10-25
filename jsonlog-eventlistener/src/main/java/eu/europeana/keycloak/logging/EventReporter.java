package eu.europeana.keycloak.logging;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;

/**
 * Created by luthien on 25/10/2021.
 */
public class EventReporter {

    private static final Logger LOG = Logger.getLogger(EventReporter.class);

    public static final  String ERROR_ICON                  = ":x:";
    public static final  String ERROR_ASCII                 = "✘";
    public static final  String OK_ICON                     = ":heavy_check_mark:";
    public static final  String OK_ASCII                    = "✓";
    public static final String SLACK_USER_DELETE_MESSAGEBODY         =
        "{\"text\":\"On %s, user %s has requested to remove their account.\\n" +
        "This has just been done automatically for those systems marked with :heavy_check_mark: :\\n\\n" +
        "[%s] Keycloak\\n" + "[%s] The User Sets API\\n" + "[:x:] The recommendation engine\\n" +
        "[:x:] Mailchimp\\n\\n" +
        "From the remaining systems (marked with :x: above) their account should be removed within 30 days (before %s).\"}";
    private static final String REQUEST_RECEIVED            = "{\"text\":\"On %s, a request was received to remove user account with ID %s.\\n\\n";
    public static final String SLACK_USER_NOTFOUND_MESSAGEBODY       =
        REQUEST_RECEIVED + "this userID could not be found in Keycloak (HTTP %d), which might indicate a problem " +
        "with the token used to send the request. Therefore the token has been logged in Kibana.\"}";
    private static final String NO_ACTION_BUT_LOGGED        = "carrying out this request.\\nNo action was taken.\\nThe user token has been logged in Kibana.";
    private static final String NO_ACTION_BUT_LOGGED_PERIOD = NO_ACTION_BUT_LOGGED + "\"}";
    public static final String SLACK_KC_COMM_ISSUE_MESSAGEBODY       =
        REQUEST_RECEIVED + "there was a problem connecting to " + "Keycloak (HTTP %d), preventing " +
        NO_ACTION_BUT_LOGGED_PERIOD;
    public static final String SLACK_SERVICE_UNAVAILABLE_MESSAGEBODY =
        REQUEST_RECEIVED + "a server error occurred which prevented " + NO_ACTION_BUT_LOGGED_PERIOD;
    private static final String NO_ACTION_LOGGED_AND_ERROR  = NO_ACTION_BUT_LOGGED + "\\n\\n[%s]\"}";
    public static final String SLACK_FORBIDDEN_MESSAGEBODY           =
        REQUEST_RECEIVED + "an authorisation/authentication problem for the embedded Keycloak User prevented " +
        NO_ACTION_LOGGED_AND_ERROR;

    private String slackWebHook;


    public EventReporter(String slackWebHook) {
        this.slackWebHook = slackWebHook;
    }

    /**
     * Configure post request sending the User delete confirmation to Slack
     *
     * @param userEmail   email address of the User to delete
     * @param kcDeleted   boolean representing success or failure deleting KC user
     * @param setsDeleted boolean representing success or failure deleting user sets
     * @param debug       boolean TRUE: force sending an email message to Slack even when the HTTP Post request
     *                    succeeds
     * @return boolean whether or not sending the message succeeded
     */
    private boolean prepareUserDeletedMessage(String userEmail, boolean kcDeleted, boolean setsDeleted, boolean debug) {
        return sendMessage(String.format(SLACK_USER_DELETE_MESSAGEBODY,
                                         LocalDate.now(),
                                         userEmail,
                                         kcDeleted ? OK_ICON : ERROR_ICON,
                                         setsDeleted ? OK_ICON : ERROR_ICON,
                                         LocalDate.now().plusDays(30)), debug);
    }

    /**
     * Send message to the Slack channel with a POST HTTP request
     *
     * @param json  contents of the messages
     * @param debug boolean true: force sending an email message to Slack even when the HTTP Post request succeeds sends
     *              an email message
     * @return boolean whether or not sending the message succeeded
     */
    private boolean sendMessage(String json, boolean debug) {
        /*
        StringEntity entity;
        HttpPost     httpPost = new HttpPost(slackWebHook);

        try {
            entity = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            LOG.error("UnsupportedEncodingException occurred while creating Slack message: {}", e.getMessage());
            return false;
        }

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
                LOG.error("Error sending Slack message: received HTTP {} response",
                          response.getStatusLine().getStatusCode());
                return false;
            }
        } catch (IOException e) {
            LOG.error("IOException occurred while sending Slack message: {}", e.getMessage());
            return false;
        }
        return !debug;
        */
        return false;
    }

    /**
     * Sends the results of the user delete request to Slack by email
     *
     * @param userEmail   email address of the User to delete
     * @param kcDeleted   boolean representing success or failure deleting KeyCloak user
     * @param setsDeleted boolean representing success or failure deleting user sets
     * @return boolean whether or not sending the message succeeded
     */
    private boolean sendUserDeletedEmail(String userEmail, boolean kcDeleted, boolean setsDeleted) {
        /*
        userDeletedSlackMail.setTo(slackEmail);
        return emailService.sendDeletedUserEmail(userDeletedSlackMail,
                                                 LocalDate.now().toString(),
                                                 userEmail,
                                                 kcDeleted ? OK_ASCII : ERROR_ASCII,
                                                 setsDeleted ? OK_ASCII : ERROR_ASCII,
                                                 LocalDate.now().plusDays(30).toString());
        */
        return false;
    }

    /**
     * Sends a report about errors that occurred while processing the user delete request to Slack using email
     *
     * @param userId    user id as found in the User Token. If this could not be retrieved, it will default to
     *                  "unknown"
     * @param errorType String defining error type to determine the contents of the email to be sent: "M" if user cannot
     *                  be found; "C" in case of errors communicating with KeyCloak; "F" if designated admin user isn't
     *                  authorised; and "U" for unknown / unexpected errors
     * @param status    int value representing the HTTP return status of
     * @return boolean whether or not sending the message succeeded
     */
    private boolean sendErrorEmail(String userId, String errorType, int status) {
        /*
        SimpleMailMessage mailTemplate;
        switch (errorType) {
            case "C":
                mailTemplate = kcCommProblemSlackMail;
                break;
            case "M":
                mailTemplate = userNotFoundSlackMail;
                break;
            case "F":
                mailTemplate = kcForbiddenSlackMail;
                break;
            case "U":
                mailTemplate = unavailableSlackMail;
                break;
            default: // shouldn't happen but just in case
                mailTemplate = unavailableSlackMail;
        }
        mailTemplate.setTo(slackEmail);
        return emailService.sendUserProblemEmail(mailTemplate, LocalDate.now().toString(), userId, status);
        */
        return false;
    }

}
