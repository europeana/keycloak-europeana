package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserDeleteConfig.ERROR_ASCII;
import static eu.europeana.keycloak.user.UserDeleteConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.OK_ASCII;
import static eu.europeana.keycloak.user.UserDeleteConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.SET_API_URL;
import static eu.europeana.keycloak.user.UserDeleteConfig.SLACK_USER_DELETE_MESSAGEBODY;
import static eu.europeana.keycloak.user.UserDeleteConfig.DEBUG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;

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
     * Retrieve UserModel for the account that's being deleted, send the Set delete request using the user token,
     * and send a confirmation message to Slack (using HTTP Post first, and email if that fails).
     * If both methods fail, the delete information is logged instead and should appear in Kibana.
     *
     * @param session       KeycloakSession
     * @param deleteEvent   user delete event captured by the custorm ProviderEventManager in
     *                      EuropeanaEventListenerProviderFactory.postInit()
     */
    public void sendUserDeleteMessage(KeycloakSession session, UserRemovedEvent deleteEvent) {
        RealmModel realm      = deleteEvent.getRealm();
        UserModel  slackUser  = session.users().getUserByUsername(this.slackUser, realm);
        UserModel  deleteUser = deleteEvent.getUser();
        logger.debug("sendUserDeleteMessage for: " + deleteUser.getUsername() + ", email: " + deleteUser.getEmail());

        boolean slackMsgSent;
        boolean setsDeleted = deleteUserSets(session, deleteUser);

        if (null != slackWebHook && !slackWebHook.equalsIgnoreCase("")){
            slackMsgSent = sendHttpMessage(formatUserDeleteMessage(deleteUser.getEmail(), true, setsDeleted));
            if (DEBUG || !slackMsgSent) {
                sendEmailMessage(session, formatUserDeleteMessage(deleteUser.getEmail(), false, setsDeleted), slackUser);
            }
        } else {
            slackMsgSent = sendEmailMessage(session, formatUserDeleteMessage(deleteUser.getEmail(), false, setsDeleted), slackUser);
        }

        if (!slackMsgSent){
            logger.error(toJson("EmailException occurred while sending Slack message: " +
                                e.getMessage()));
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
     * @return boolean whether or not sending the message succeeded
     */
    private boolean sendHttpMessage(String message) {
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
        return true;
    }


    /**
     * Sends the results of the user delete request to Slack by email
     *
     * @param session   email address of the User to delete
     * @param message   message
     * @param slackUser UserModel
     * @return boolean whether or not sending the message succeeded
     */
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

    private boolean deleteUserSets(KeycloakSession session, UserModel deleteUser) {
        String userToken = getAccessToken(session, deleteUser);
        HttpDelete httpDelete = new HttpDelete(SET_API_URL);
        httpDelete.setHeader("Authorization", "Bearer " + userToken);

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                logger.error("Error sending User Sets delete request: received HTTP " +
                          response.getStatusLine().getStatusCode() + " response");
                return false;
            }
        } catch (IOException e) {
            logger.error("IOException occurred while sending User Sets delete request: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getAccessToken(KeycloakSession session, UserModel deleteUser) {
        KeycloakContext keycloakContext = session.getContext();

        AccessToken token = new AccessToken();
        token.subject(deleteUser.getId());
        token.issuer(Urls.realmIssuer(keycloakContext.getUri().getBaseUri(), keycloakContext.getRealm().getName()));
        token.issuedNow();
        token.expiration((int) (token.getIat() + 60L)); //Lifetime of 60 seconds

        KeyWrapper key = session.keys().getActiveKey(keycloakContext.getRealm(), KeyUse.SIG, "RS256");

        return new JWSBuilder().kid(key.getKid()).type("JWT").jsonContent(token).sign(new AsymmetricSignatureSignerContext(key));
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
