package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserRemovedConfig.CLIENT_ID;
import static eu.europeana.keycloak.user.UserRemovedConfig.CLIENT_SECRET;
import static eu.europeana.keycloak.user.UserRemovedConfig.DEBUG_LOGS;
import static eu.europeana.keycloak.user.UserRemovedConfig.DELETE_MGR_ID;
import static eu.europeana.keycloak.user.UserRemovedConfig.DELETE_MGR_PW;
import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.GRANT_TYPE;
import static eu.europeana.keycloak.user.UserRemovedConfig.HTTP_AND_EMAIL_FAILED;
import static eu.europeana.keycloak.user.UserRemovedConfig.HTTP_FAILED_TRYING_EMAIL;
import static eu.europeana.keycloak.user.UserRemovedConfig.OIDTokenURL;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ASCII;
import static eu.europeana.keycloak.user.UserRemovedConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserRemovedConfig.SCOPE;
import static eu.europeana.keycloak.user.UserRemovedConfig.SENDING_CONFIRM_MSG_SLACK;
import static eu.europeana.keycloak.user.UserRemovedConfig.SET_API_URL;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_MSG_SENT;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_USER_DELETE_MESSAGEBODY;
import static eu.europeana.keycloak.user.UserRemovedConfig.USER_SETS_DELETED;
import static eu.europeana.keycloak.user.UserRemovedConfig.USER_SETS_NOT_DELETED;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.apache.http.HttpEntity;
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
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;

//import org.keycloak.representations.

/**
 * Created by luthien on 25/10/2021.
 * Processes a User Removed event caught by EuropeanaEventListenerProviderFactory
 */
public class UserRemovedMessageHandler {

//        public static final boolean DEBUG = true;
//    public static final boolean DEBUG = false;

    private static final Logger LOG = Logger.getLogger(UserRemovedMessageHandler.class);
    private final CloseableHttpClient httpClient;
    private final String              slackWebHook;
    private final String              slackUser;
    private final String prefix;
    private boolean setsDeleteOK       = false;
    private boolean slackHttpMessageOK  = false;
    private boolean slackEmailMessageOK = false;

    private String userSetToken;

    public UserRemovedMessageHandler(String slackWebHook, String slackUser, Logger logger, String prefix) {
        this.slackWebHook = slackWebHook;
        this.slackUser    = slackUser;
        this.prefix       = prefix;
        httpClient        = HttpClients.createDefault();
    }

    @PreDestroy
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            LOG.info("HTTP client closed.");
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

        if (getUserSetToken()){
            setsDeleteOK = sendUserSetDeleteRequest(deleteEvent, userSetToken);
        }

        if (DEBUG_LOGS) {
            LOG.info(toJson(deleteEvent, setsDeleteOK ? USER_SETS_DELETED : USER_SETS_NOT_DELETED));
            LOG.info(toJson(deleteEvent, SENDING_CONFIRM_MSG_SLACK));
        }

        if (null != slackWebHook && !slackWebHook.equalsIgnoreCase("")) {
            slackHttpMessageOK = sendSlackHttpMessage(deleteEvent);
        }

        if (!slackHttpMessageOK) {
            if (DEBUG_LOGS) LOG.info(toJson(deleteEvent, HTTP_FAILED_TRYING_EMAIL));
            slackEmailMessageOK = sendSlackEmailMessage(session, slackUser, deleteEvent);
        }

        if (slackHttpMessageOK || slackEmailMessageOK) {
            LOG.info(toJson(deleteEvent, SLACK_MSG_SENT));
        } else {
            LOG.error(toJson(deleteEvent, HTTP_AND_EMAIL_FAILED));
        }
    }

    /**
     * Retrieves the token required to authenticate the Userset delete request and assigns that to userSetToken
     *
     * @return boolean: TRUE  if the token request was answered with HTTP 200 response was received and contained
     *                        an "access_token" value
     *                  FALSE for any other status or error condition
     */
    public boolean getUserSetToken() {

            String responseString;
            HttpPost     httpPost = new HttpPost(OIDTokenURL);
            if (DEBUG_LOGS){
                LOG.info("Auth server OID url: " + OIDTokenURL);
            }
            ArrayList<NameValuePair> postParameters;

            postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
            postParameters.add(new BasicNameValuePair("username", DELETE_MGR_ID));
            postParameters.add(new BasicNameValuePair("password", DELETE_MGR_PW));
            postParameters.add(new BasicNameValuePair("client_id", CLIENT_ID));
            postParameters.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
            postParameters.add(new BasicNameValuePair("scope", SCOPE));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }

            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    LOG.error("Error sending POST request, received HTTP " +
                              response.getStatusLine().getStatusCode() +
                              " response");
                    return false;
                } else {
                    HttpEntity entity = response.getEntity();
                    responseString = EntityUtils.toString(entity);
                    userSetToken = readJsonValue(responseString, "access_token");
                    if (isNotBlank(userSetToken)){
                        if (DEBUG_LOGS) LOG.info("User set access token: " + userSetToken);
                        return true;
                    } else {
                        LOG.error("No access token found in response from Keycloak: " + responseString);
                        return false;
                    }
                }
            } catch (Exception e) {
                LOG.error(e);
                return false;
            }
    }

    /**
     * Send User set delete request to the user set API
     *
     * @param deleteEvent   UserRemovedEvent containing the removed user data
     * @param token   the trusted client token required to authorise the request
     * @return boolean: TRUE  if a HTTP 204 response was received on the Userset Delete request
     *                  FALSE if any other status was recieved or a IOException occurred
     */
    private boolean sendUserSetDeleteRequest(UserRemovedEvent deleteEvent, String token) {

        UserModel  deleteUser = deleteEvent.getUser();
        String setsApiRequest  = SET_API_URL + "?creator=" + deleteUser.getId();
        if (DEBUG_LOGS){
            setsApiRequest += "&profile=debug&includeErrorStack=true";
            LOG.info(setsApiRequest);
        }
        HttpDelete httpDelete = new HttpDelete(setsApiRequest);

        httpDelete.setHeader("Authorization", "Bearer " + token);

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                LOG.info(toJson(deleteEvent,
                                "Usersets delete request successful: received HTTP " +
                                response.getStatusLine().getStatusCode() +
                                " response"));
                return true;
            } else {
                if (DEBUG_LOGS) {
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity);
                    LOG.error("Usersets delete request was not successful: received HTTP " +
                              response.getStatusLine().getStatusCode() +
                              " response. Response body: " + responseBody);
                } else {
                    LOG.error(toJson(deleteEvent,
                                 "Usersets delete request was not successful: received HTTP " +
                                 response.getStatusLine().getStatusCode() +
                                 " response"));
                }
                return false;
            }
        } catch (IOException e) {
            if (DEBUG_LOGS) {
                LOG.error(e);
            } else {
                LOG.error(toJson(deleteEvent,
                             "IOException occurred while sending delete request by HTTP: " + e.getMessage()));
            }
            return false;
        }
    }

    private String formatUserRemovedMessage(UserRemovedEvent deleteEvent, boolean useIcon) {
        UserModel  deleteUser = deleteEvent.getUser();
        String okString    = useIcon ? OK_ICON : OK_ASCII;
        String errorString = useIcon ? ERROR_ICON : ERROR_ASCII;
        return String.format(SLACK_USER_DELETE_MESSAGEBODY,
                             LocalDate.now(),
                             deleteUser.getEmail(),
                             okString,
                             setsDeleteOK ? okString : errorString,
                             LocalDate.now().plusDays(30));
    }

    /**
     * Send message to the Slack channel with a POST HTTP request
     *
     * @param deleteEvent UserRemovedEvent containing the removed user data
     * @return boolean: TRUE  if HTTP 200 was received after sending the message
     *                  FALSE in case of any other status or an error occurring
     */
    private boolean sendSlackHttpMessage(UserRemovedEvent deleteEvent) {

        String message = formatUserRemovedMessage(deleteEvent, true);

        StringEntity entity;
        HttpPost     httpPost = new HttpPost(slackWebHook);

        try {
            entity = new StringEntity(message);
        } catch (UnsupportedEncodingException e) {
            LOG.errorf("UnsupportedEncodingException occurred while creating Slack message", e);
            return false;
        }

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOG.info(toJson(deleteEvent,
                                "Slack message sent successfully: received HTTP " +
                                response.getStatusLine().getStatusCode() +
                                " response"));
                return true;
            } else {
                LOG.error(toJson(deleteEvent,
                                 "Error sending Slack message: received HTTP " +
                                 response.getStatusLine().getStatusCode() +
                                 " response"));
                return false;
            }
        } catch (IOException e) {
            LOG.error(e);
            return false;
        }
    }


    /**
     * Sends the results of the user delete request to Slack by email
     *
     * @param session   email address of the User to delete
     * @param slackUser  Slack user in Keycloak
     * @param deleteEvent UserRemovedEvent containing the removed user data
     * @return boolean TRUE  if the email was sent without an error
     *                 FALSE if an EmailException error occurred
     */
    private boolean sendSlackEmailMessage(KeycloakSession session, UserModel slackUser, UserRemovedEvent deleteEvent) {

        String message = formatUserRemovedMessage(deleteEvent, false);
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
            return true;
        } catch (EmailException e) {
            LOG.error(toJson(deleteEvent,
                             "EmailException occurred while sending Slack message by email: " + e.getMessage()));
            return false;
        }
    }

    private String toJson(UserRemovedEvent deleteEvent, String msg) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "USER_DELETE_EVENT");

        if (deleteEvent.getRealm() != null) {
            obj.add("realmName", deleteEvent.getRealm().getName());
        }

        if (deleteEvent.getUser() != null) {
            if (isNotBlank(deleteEvent.getUser().getId())) {
                obj.add("userId", deleteEvent.getUser().getId());
            }
            if (isNotBlank(deleteEvent.getUser().getEmail())) {
                obj.add("userEmail", deleteEvent.getUser().getEmail());
            }
            if (isNotBlank(deleteEvent.getUser().getUsername())) {
                obj.add("userName", deleteEvent.getUser().getUsername());
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

    private String readJsonValue(String jsonString, String key){
        String token;
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();
        token = jsonObject.getString(key);
        jsonReader.close();
        return token;
    }

}
