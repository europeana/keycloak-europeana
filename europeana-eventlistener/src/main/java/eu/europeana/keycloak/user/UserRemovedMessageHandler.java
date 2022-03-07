package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserRemovedMessage.*;
import static eu.europeana.keycloak.user.UserRemovedEnvVariables.*;

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
 * Created by luthien on 25/10/2021. Processes a User Removed event caught by EuropeanaEventListenerProviderFactory
 */
public class UserRemovedMessageHandler {

    private static final Logger              LOG                 = Logger.getLogger(UserRemovedMessageHandler.class);
    private final        CloseableHttpClient httpClient;
    private final        String              slackWebHook;
    private final        String              slackUserName;
    private final        String              prefix;
    private              boolean             setsDeleteOK        = false;
    private              boolean             slackHttpMessageOK  = false;
    private              boolean             slackEmailMessageOK = false;

    private String userSetToken;

    public UserRemovedMessageHandler(String slackWebHook, String slackUserName, Logger logger, String prefix) {
        this.slackWebHook  = slackWebHook;
        this.slackUserName = slackUserName;
        this.prefix        = prefix;
        httpClient         = HttpClients.createDefault();
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
        RealmModel realm          = deleteEvent.getRealm();
        UserModel  slackUserModel = session.users().getUserByUsername(this.slackUserName, realm);

        if (getUserSetToken()) {
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
            if (DEBUG_LOGS) {
                LOG.info(toJson(deleteEvent, HTTP_FAILED_TRYING_EMAIL));
            }
            slackEmailMessageOK = sendSlackEmailMessage(session, slackUserModel, deleteEvent);
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
     * @return boolean: TRUE  if the token request was answered with HTTP 200 response was received and contained an
     * "access_token" value FALSE for any other status or error condition
     */
    public boolean getUserSetToken() {

        String   responseString;
        HttpPost httpPost = new HttpPost(OIDTokenURL);
        if (DEBUG_LOGS) {
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
            LOG.error(e);
            return false;
        }

        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.errorf(USERSET_TOKEN_POST_ERROR, response.getStatusLine().getStatusCode());
                return false;
            } else {
                HttpEntity entity = response.getEntity();
                responseString = EntityUtils.toString(entity);
                userSetToken   = readJsonValue(responseString, "access_token");
                if (isNotBlank(userSetToken)) {
                    if (DEBUG_LOGS) {
                        LOG.infof(ACCESS_TOKEN, userSetToken);
                    }
                    return true;
                } else {
                    LOG.errorf(NO_ACCESS_TOKEN_FOUND, responseString);
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
     * @param deleteEvent UserRemovedEvent containing the removed user data
     * @param token       the trusted client token required to authorise the request
     * @return boolean: TRUE  if a HTTP 204 response was received on the Userset Delete request FALSE if any other
     * status was received or a IOException occurred
     */
    private boolean sendUserSetDeleteRequest(UserRemovedEvent deleteEvent, String token) {

        UserModel deleteUser     = deleteEvent.getUser();
        String    setsApiRequest = SET_API_URL + "?creator=" + deleteUser.getId();
        if (DEBUG_LOGS) {
            setsApiRequest += "&profile=debug&includeErrorStack=true";
            LOG.info(setsApiRequest);
        }
        HttpDelete httpDelete = new HttpDelete(setsApiRequest);

        httpDelete.setHeader("Authorization", "Bearer " + token);

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                LOG.info(toJson(deleteEvent, String.format(USERSET_DELETE_RESULT,
                                                           "",
                                                           response.getStatusLine().getStatusCode())));
                return true;
            } else {
                if (DEBUG_LOGS) {
                    HttpEntity entity       = response.getEntity();
                    String     responseBody = EntityUtils.toString(entity);
                    LOG.errorf(USERSET_DELETE_NO_SUCCESS,
                               response.getStatusLine().getStatusCode(),
                               responseBody);
                } else {
                    LOG.error(toJson(deleteEvent, String.format(USERSET_DELETE_RESULT,
                                                                "not ",
                                                                response.getStatusLine().getStatusCode())));
                }
                return false;
            }
        } catch (IOException e) {
            if (DEBUG_LOGS) {
                LOG.error(USERSET_HTTP_IOEXCEPT, e);
            } else {
                LOG.error(toJson(deleteEvent, USERSET_HTTP_IOEXCEPT + e.getMessage()));
            }
            return false;
        }
    }

    private String formatUserRemovedMessage(UserRemovedEvent deleteEvent, boolean useIcon) {
        UserModel deleteUser  = deleteEvent.getUser();
        String    okString    = useIcon ? OK_ICON : OK_ASCII;
        String    errorString = useIcon ? ERROR_ICON : ERROR_ASCII;
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
     * @return boolean: TRUE  if HTTP 200 was received after sending the message FALSE in case of any other status or an
     * error occurring
     */
    private boolean sendSlackHttpMessage(UserRemovedEvent deleteEvent) {

        String message = formatUserRemovedMessage(deleteEvent, true);

        StringEntity entity;
        HttpPost     httpPost = new HttpPost(slackWebHook);

        try {
            entity = new StringEntity(message);
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOGS) {
                LOG.error(SLACK_MSG_UNSUPPORTED_EXCEPT, e);
            } else {
                LOG.error(toJson(deleteEvent, SLACK_MSG_UNSUPPORTED_EXCEPT + e.getMessage()));
            }
            return false;
        }

        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOG.info(toJson(deleteEvent, String.format(SLACK_MSG_RESULT_STATUS,
                                                           "was",
                                                           response.getStatusLine().getStatusCode())));
                return true;
            } else {
                LOG.info(toJson(deleteEvent, String.format(SLACK_MSG_RESULT_STATUS,
                                                           "not",
                                                           response.getStatusLine().getStatusCode())));
                return false;
            }
        } catch (IOException e) {
            if (DEBUG_LOGS) {
                LOG.error(SLACK_MSG_HTTP_IOEXCEPT, e);
            } else {
                LOG.error(toJson(deleteEvent, SLACK_MSG_HTTP_IOEXCEPT + e.getMessage()));
            }
            return false;
        }
    }


    /**
     * Sends the results of the user delete request to Slack by email
     *
     * @param session        email address of the User to delete
     * @param slackUserModel Slack user in Keycloak
     * @param deleteEvent    UserRemovedEvent containing the removed user data
     * @return boolean TRUE  if the email was sent without an error FALSE if an EmailException error occurred
     */
    private boolean sendSlackEmailMessage(KeycloakSession session, UserModel slackUserModel,
                                          UserRemovedEvent deleteEvent) {

        String                     message        = formatUserRemovedMessage(deleteEvent, false);
        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        UserModel                  deleteUser     = deleteEvent.getUser();

        try {
            LOG.info(toJson(deleteEvent,
                            "Sending email to Slack user: " + slackUserModel.getEmail()));
            senderProvider.send(
                session.getContext().getRealm().getSmtpConfig(),
                slackUserModel,
                "User account for Keycloak user with ID: " + deleteUser.getId(),
                message,
                message);
            return true;
        } catch (EmailException e) {
            if (DEBUG_LOGS) {
                LOG.error(SLACK_MSG_EMAIl_EXCEPT, e);
            } else {
                LOG.error(toJson(deleteEvent, SLACK_MSG_EMAIl_EXCEPT + e.getMessage()));
            }
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

    private String readJsonValue(String jsonString, String key) {
        String     token;
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();
        token = jsonObject.getString(key);
        jsonReader.close();
        return token;
    }

}
