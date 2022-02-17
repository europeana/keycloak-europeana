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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;

//import org.keycloak.representations.

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

    private final String AUTHSERVERURL = "http://localhost:8080/auth/";
    private final String OIDTokenURL = AUTHSERVERURL + "realms/europeana/protocol/openid-connect/token";



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
//        getTrustedClientToken(session, deleteUser);
        getKCClient();

        // TODO this is still to be fixed, commenting out now to prevent error messages
        areSetsDeleted = deleteUserSets(session, deleteUser);

        if (areSetsDeleted) {
            LOG.info(toJson(deleteEvent, "User sets deleted."));
        }

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
//        String     userToken  = getAccessToken(session);
        HttpDelete httpDelete = new HttpDelete(SET_API_URL);
//        httpDelete.setHeader("Authorization", "Bearer " + userToken);

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

//    private String getAccessToken(KeycloakSession session) {
//
//        KeycloakContext context = session.getContext();
//        RealmModel realm = context.getRealm();
//        UserModel deleteManagerUser = session.users().getUserByUsername("keycloak_user_delete", realm);
////        UserModel user = session.users().getUserById(realm, token.getSubject());
////        if (user == null) {
////            return Response.status(UNAUTHORIZED).build();
////        }
//
//        AccessToken token = new AccessToken();
//        token.subject(deleteManagerUser.getId());
//        token.issuer(Urls.realmIssuer(context.getUri().getBaseUri(), context.getRealm().getName()));
//        token.issuedNow();
//        token.setScope("usersets");
////        token.
//        token.expiration((int) (token.getIat() + 60L)); //Lifetime of 60 seconds
//
//        KeyWrapper key = session.keys().getActiveKey(context.getRealm(), KeyUse.SIG, "RS256");
//
//        return new JWSBuilder().kid(key.getKid()).type("JWT").jsonContent(token).sign(
//            new AsymmetricSignatureSignerContext(key));
//    }

    private void getTrustedClientToken(KeycloakSession session, UserModel deleteUser) {


//        Keycloak keycloak = KeycloakBuilder.builder()
//                                           .serverUrl("http://localhost:8080/auth")
//                                           .realm("master")
//                                           .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
//                                           .clientId("Account2")
//                                           .clientSecret("cTXdyvBqrCuZY07k1iRHubDlZkkqlcmC")
//                                           .build();

//        Keycloak keycloak = Keycloak.getInstance(
//            "http://localhost:8080/auth",
//            "master",
//            "admin",
//            "password",
//            "admin-cli");
//        RealmRepresentation realm = keycloak.realm("master").toRepresentation();


        HttpPost     httpPost = new HttpPost(OIDTokenURL);
        ArrayList<NameValuePair> postParameters;

        postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("grant_type", "password"));
        postParameters.add(new BasicNameValuePair("username", "fifi"));
        postParameters.add(new BasicNameValuePair("password", "finufi"));
        postParameters.add(new BasicNameValuePair("client_id", "test_client_userset"));
        postParameters.add(new BasicNameValuePair("client_secret", "3dde4cdb-ee54-42f2-a198-5f38a753f5cc"));
        postParameters.add(new BasicNameValuePair("scope", "usersets"));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

//        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.error("Error sending POST request, received HTTP " +
                                 response.getStatusLine().getStatusCode() +
                                 " response");

            }
            LOG.info(response.toString());
            System.out.println(response.toString());
        } catch (Exception e) {
            LOG.error("IOException occurred while sending Slack message by HTTP: " + e.getMessage());

        } finally {
            LOG.info("Finally");

        }
    }


    public void getKCClient() {
        String realm = "europeana";
        // idm-client needs to allow "Direct Access Grants: Resource Owner Password Credentials Grant"
        String clientId = "test_client_userset";
        String clientSecret = "3dde4cdb-ee54-42f2-a198-5f38a753f5cc";

        // Client "idm-helper" needs service-account with at least "manage-users, view-clients, view-realm, view-users" roles for "realm-management"
        Keycloak keycloak = KeycloakBuilder.builder() //
                                           .serverUrl(AUTHSERVERURL) //
                                           .realm(realm) //
                                           .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                                           .clientId(clientId) //
                                           .clientSecret(clientSecret).build();

        List<UserRepresentation> users = keycloak.realm(realm).users().search("userset", true);
        System.out.println("users: " + users.toString());
        //userset

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
