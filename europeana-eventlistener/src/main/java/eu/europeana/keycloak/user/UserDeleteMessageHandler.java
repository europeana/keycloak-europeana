package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserDeleteConfig.ERROR_ASCII;
import static eu.europeana.keycloak.user.UserDeleteConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.OK_ASCII;
import static eu.europeana.keycloak.user.UserDeleteConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.SLACK_USER_DELETE_MESSAGEBODY;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;

/**
 * Created by luthien on 25/10/2021.
 */
public class UserDeleteMessageHandler {

    private final CloseableHttpClient httpClient;
    private final String              slackWebHook;
    private final String              slackUser;
    private final String              prefix;
    Logger logger;

    public UserDeleteMessageHandler(String slackWebHook, String slackUser, Logger logger, String prefix) {
        this.slackWebHook = slackWebHook;
        this.slackUser    = slackUser;
        this.logger       = logger;
        this.prefix       = prefix;
        httpClient        = HttpClients.createDefault();
    }

    @PreDestroy
    public void close() throws IOException {
        if (httpClient != null) {
            logger.info("Closing http client ...");
            httpClient.close();
        }
    }


    /**
     * Configure post request sending the User delete confirmation to Slack
     *
     * @param session       KeycloakSession
     * @param deleteEvent   user delete event captured by the keycloak ProviderEventManager
     * @return boolean whether or not sending the message succeeded
     */
    public void sendUserDeleteMessage(KeycloakSession session, UserRemovedEvent deleteEvent) {
        RealmModel realm      = deleteEvent.getRealm();
        UserModel  slackUser  = session.users().getUserByUsername(this.slackUser, realm);
        UserModel  deleteUser = deleteEvent.getUser();
        logger.debug("sendUserDeleteMessage for: " + deleteUser.getUsername() + ", email: " + deleteUser.getEmail());

        boolean setsDeleted = false;
        boolean debug = true;

        if (null != slackWebHook && !slackWebHook.equalsIgnoreCase("")){
            sendHttpMessage(formatUserDeleteMessage(deleteUser.getEmail(), true, setsDeleted), debug);
        } else {
            sendEmailMessage(session, formatUserDeleteMessage(deleteUser.getEmail(), false, setsDeleted), slackUser);
        }
    }

    private String formatUserDeleteMessage(String email, boolean useIcon, boolean setsDeleted){
        String okString = useIcon? OK_ICON : OK_ASCII;
        String errorString = useIcon? ERROR_ICON : ERROR_ASCII;
        return String.format(SLACK_USER_DELETE_MESSAGEBODY,
                             LocalDate.now(),
                             email,
                             okString,
                             setsDeleted ? okString : errorString,
                             LocalDate.now().plusDays(30));
    }

    /**
     * Send message to the Slack channel with a POST HTTP request
     *
     * @param message  contents of the messages
     * @param debug boolean true: force sending an email message to Slack even when the HTTP Post request succeeds sends
     *              an email message
     * @return boolean whether or not sending the message succeeded
     */
    private boolean sendHttpMessage(String message, boolean debug) {
        StringEntity entity;
        HttpPost     httpPost = new HttpPost(slackWebHook);

        try {
            entity = new StringEntity(message);
        } catch (UnsupportedEncodingException e) {
            logger.errorf("UnsupportedEncodingException occurred while creating Slack message: {}", e.getMessage());
            return false;
        }

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error(toJson("Error sending Slack message: received HTTP " +
                                    response.getStatusLine().getStatusCode() +
                                    " response"));
                return false;
            }
        } catch (IOException e) {
            logger.error(toJson("IOException occurred while sending Slack message: " +
                                e.getMessage()));
            return false;
        }
        return !debug;
    }


    private boolean sendEmailMessage (KeycloakSession session, String message, UserModel slackUser){
        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        try {
            logger.info(toJson("sending email to: " + slackUser.getEmail()));
            senderProvider.send(
                session.getContext().getRealm().getSmtpConfig(),
                slackUser,
                "test",
                message,
                message);
        } catch (EmailException e) {
            logger.error(toJson("EmailException occurred while sending Slack message: " +
                                e.getMessage()));
            return false;
        }
        return true;
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

    private String toJson(String msg) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "MESSAGE_HANDLER_EVENT");

        obj.add("message", msg);

        return prefix + obj.build().toString();
    }

}
