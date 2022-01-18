package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.SET_API_URL;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_USER_DELETE_MESSAGEBODY;

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

    //    public static final boolean DEBUG = true;
    public static final boolean DEBUG = false;

    private static final Logger LOG = Logger.getLogger(UserRemovedMessageHandler.class);
    private final CloseableHttpClient httpClient;
    private final String              slackWebHook;
    private final String              slackUser;
    private final String              prefix;
    boolean areSetsDeleted      = false;
    boolean slackHttpSendError  = false;
    boolean slackEmailSendError = false;


    public UserRemovedMessageHandler(String slackWebHook, String slackUser, Logger logger, String prefix) {
        this.slackWebHook = slackWebHook;
        this.slackUser    = slackUser;
        this.prefix       = prefix;
        httpClient        = HttpClients.createDefault();
    }

    @PreDestroy
    public void close() throws IOException {
        if (httpClient != null) {
            LOG.info("Closing http client ...");
            httpClient.close();
        }
    }


    /**
     * Retrieve UserModel for the account that's being deleted, send the Set delete request using the user token, and
     * send a confirmation message to Slack (using HTTP Post first, and email if that fails). If both methods fail, the
     * delete information is logged instead and should appear in Kibana.
     *
     * @param session     KeycloakSession
     * @param deleteEvent user delete event captured by the custorm ProviderEventManager in
     *                    EuropeanaEventListenerProviderFactory.postInit()
     */
    public void handleUserRemoveEvent(KeycloakSession session, UserRemovedEvent deleteEvent) {
        RealmModel realm      = deleteEvent.getRealm();
        UserModel  slackUser  = session.users().getUserByUsername(this.slackUser, realm);
        UserModel  deleteUser = deleteEvent.getUser();

//        LOG.info(toJson(deleteEvent, "User account removed, trying to delete user sets ..."));

        // TODO this is still to be fixed, commenting out now to prevent error messages
//        areSetsDeleted = deleteUserSets(session, deleteUser);

//        if (areSetsDeleted)) {
//            LOG.info(toJson(deleteEvent, "User sets deleted."));
//        }

        LOG.info(toJson(deleteEvent, "Sending confirmation message to Slack"));

        if (null != slackWebHook && !slackWebHook.equalsIgnoreCase("")) {
            slackHttpSendError = sendHttpMessage(formatUserRemovedMessage(
                                                  deleteUser.getEmail(),
                                                  true),
                                                 deleteEvent);
        }

        if (slackHttpSendError) {
            LOG.error(toJson(deleteEvent, "Error occurred trying to send the message over HTTP, now trying to " +
                                          "send it via email ..."));
        }

        if (DEBUG || slackHttpSendError) {
            slackEmailSendError = sendEmailMessage(session,
                                                   formatUserRemovedMessage(deleteUser.getEmail(),
                                                                         false),
                                                   slackUser,
                                                   deleteEvent);
        }

        if (slackEmailSendError) {
            LOG.error(toJson(deleteEvent,
                             "!IMPORTANT! User account was removed, but failed to send confirmation message to Slack. " +
                             "Please notify the Slack delete_user_account channel, providing details about the " +
                             "error and the deleted user, so they can handle the issue manually."));
        } else {
            LOG.info(toJson(deleteEvent, "Confirmation message was sent to Slack"));
        }

    }

    // TODO fix this:  this access token can't be used for this because of
    // 1) it doesn't have the necessary Client info (see the "create and delete userset JMeter test for how to request it)
    // 2) it's probably too late to use a token for the user in this class because it's already being deleted
    // so we should probably think of something else in the Set API
    private boolean deleteUserSets(KeycloakSession session, UserModel deleteUser) {
        String     userToken  = getAccessToken(session, deleteUser);
        HttpDelete httpDelete = new HttpDelete(SET_API_URL);
        httpDelete.setHeader("Authorization", "Bearer " + userToken);

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                LOG.error("Error sending User Sets delete request: received HTTP " +
                          response.getStatusLine().getStatusCode() + " response");
                return false;
            }
        } catch (IOException e) {
            LOG.error("IOException occurred while sending User Sets delete request: " + e.getMessage());
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

        return new JWSBuilder().kid(key.getKid()).type("JWT").jsonContent(token).sign(
            new AsymmetricSignatureSignerContext(key));
    }

    private String formatUserRemovedMessage(String email, boolean useIcon) {
        String okString    = useIcon ? OK_ICON : OK_ASCII;
        String errorString = useIcon ? ERROR_ICON : ERROR_ASCII;
        return String.format(SLACK_USER_DELETE_MESSAGEBODY,
                             LocalDate.now(),
                             email,
                             okString,
                             areSetsDeleted ? okString : errorString,
                             LocalDate.now().plusDays(30));
    }

    /**
     * Send message to the Slack channel with a POST HTTP request
     *
     * @param message contents of the messages
     * @return boolean: TRUE if an error occurred, preventing the message to be sent
     *                  FALSE if the message was sent without an error
     */
    private boolean sendHttpMessage(String message, UserRemovedEvent deleteEvent) {
        StringEntity entity;
        HttpPost     httpPost = new HttpPost(slackWebHook);

        try {
            entity = new StringEntity(message);
        } catch (UnsupportedEncodingException e) {
            LOG.errorf("UnsupportedEncodingException occurred while creating Slack message: {}", e.getMessage());
            return true;
        }

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.error(toJson(deleteEvent,
                                 "Error sending Slack message: received HTTP " +
                                 response.getStatusLine().getStatusCode() +
                                 " response"));
                return false;
            }
        } catch (IOException e) {
            LOG.error(toJson(deleteEvent,
                             "IOException occurred while sending Slack message by HTTP: " + e.getMessage()));
            return true;
        }
        return false;
    }


    /**
     * Sends the results of the user delete request to Slack by email
     *
     * @param session   email address of the User to delete
     * @param message   message
     * @param slackUser UserModel
     * @return boolean TRUE if an error occurred, preventing the email to be sent
     *                 FALSE if the email was sent without an error
     */
    private boolean sendEmailMessage(KeycloakSession session, String message, UserModel slackUser,
                                     UserRemovedEvent deleteEvent) {
        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        try {
            LOG.info(toJson(deleteEvent,
                            "Sending email to Slack user: " + slackUser.getEmail()));
            senderProvider.send(
                session.getContext().getRealm().getSmtpConfig(),
                slackUser,
                "test",
                message,
                message);
        } catch (EmailException e) {
            LOG.error(toJson(deleteEvent,
                             "EmailException occurred while sending Slack message by email: " + e.getMessage()));
            return true;
        }
        return false;
    }

    private String toJson(UserRemovedEvent event, String msg) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "USER_DELETE_EVENT");

        if (event.getRealm() != null) {
            obj.add("realmName", event.getRealm().getName());
        }

        if (event.getUser() != null) {
            if (isNotBlank(event.getUser().getId())) {
                obj.add("userId", event.getUser().getId());
            }
            if (isNotBlank(event.getUser().getEmail())) {
                obj.add("userEmail", event.getUser().getEmail());
            }
            if (isNotBlank(event.getUser().getUsername())) {
                obj.add("userName", event.getUser().getUsername());
            }
        }

        if (msg != null) {
            obj.add("message", msg);
        }

        return prefix + obj.build().toString();
    }

    private boolean isNotBlank(String str) {
        return (null != str && !str.isEmpty() && !str.isBlank());
    }

}
