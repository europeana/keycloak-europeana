package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.SET_API_URL;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_USER_DELETE_MESSAGEBODY;
import static eu.europeana.keycloak.user.UserRemovedConfig.DEBUG;

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
public class UserRemovedMessageHandler {

    private final CloseableHttpClient httpClient;
    private final String              slackWebHook;
    private final String              slackUser;
    private final String              prefix;
    Logger logger;

    public UserRemovedMessageHandler(String slackWebHook, String slackUser, Logger logger, String prefix) {
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
    public void handleUserRemoveEvent(KeycloakSession session, UserRemovedEvent deleteEvent) {
        RealmModel realm      = deleteEvent.getRealm();
        UserModel  slackUser  = session.users().getUserByUsername(this.slackUser, realm);
        UserModel  deleteUser = deleteEvent.getUser();

        boolean slackHttpSent = false;
        boolean slackEmailSent = false;

        logger.info(toJson(deleteEvent,
                           "User account removed, trying to delete user sets ..."));
        boolean setsDeleted = deleteUserSets(session, deleteUser);

        if (setsDeleted) {
            logger.info(toJson(deleteEvent,
                               "User sets deleted."));
        }
        logger.info(toJson(deleteEvent,
                           "User account removed, sending confirmation message to Slack"));

        if (null != slackWebHook && !slackWebHook.equalsIgnoreCase("")){
            slackHttpSent = sendHttpMessage(formatUserRemovedMessage(deleteUser.getEmail(), true, setsDeleted), deleteEvent);
        }

        if (DEBUG || !slackHttpSent) {
            slackEmailSent = sendEmailMessage(session, formatUserRemovedMessage(deleteUser.getEmail(), false, setsDeleted), slackUser, deleteEvent);
        }

        if (!slackEmailSent){
            logger.error(toJson(deleteEvent,
                                "User account was removed but failed to send confirmation message to Slack"));
        }

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

    private String formatUserRemovedMessage(String email, boolean useIcon, boolean setsDeleted){
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
    private boolean sendHttpMessage(String message, UserRemovedEvent deleteEvent) {
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
                logger.error(toJson(deleteEvent,
                                    "Error sending Slack message: received HTTP " +
                                    response.getStatusLine().getStatusCode() +
                                    " response"));
                return false;
            }
        } catch (IOException e) {
            logger.error(toJson(deleteEvent,
                                "IOException occurred while sending Slack message by HTTP: " + e.getMessage()));
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
    private boolean sendEmailMessage (KeycloakSession session, String message, UserModel slackUser, UserRemovedEvent deleteEvent) {
        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        try {
            logger.info(toJson(deleteEvent,
                               "Sending email to Slack user: " + slackUser.getEmail()));
            senderProvider.send(
                session.getContext().getRealm().getSmtpConfig(),
                slackUser,
                "test",
                message,
                message);
        } catch (EmailException e) {
            logger.error(toJson(deleteEvent,
                                "EmailException occurred while sending Slack message by email: " + e.getMessage()));
            return false;
        }
        return true;
    }

    private String toJson(UserRemovedEvent event, String msg) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "USER_DELETE_EVENT");

        if (event.getRealm() != null) {
            obj.add("realmName", event.getRealm().getDisplayName());
        }

        if (event.getUser() != null) {
            if (isNotBlank(event.getUser().getId())){
                obj.add("userId", event.getUser().getId());
            }
            if (isNotBlank(event.getUser().getEmail())){
                obj.add("userEmail", event.getUser().getEmail());
            }
            if (isNotBlank(event.getUser().getUsername())){
                obj.add("userName", event.getUser().getUsername());
            }
        }

        if (msg != null) {
            obj.add("message", msg);
        }

        return prefix + obj.build().toString();
    }

    private boolean isNotBlank(String str){
        return (null != str && !str.isEmpty() && !str.isBlank());
    }

}
