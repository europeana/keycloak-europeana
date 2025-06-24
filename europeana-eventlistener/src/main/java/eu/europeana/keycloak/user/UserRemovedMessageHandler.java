package eu.europeana.keycloak.user;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.ArrayList;
import jakarta.annotation.PreDestroy;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.util.List;
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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import static eu.europeana.keycloak.user.UserRemovedConfig.*;


/**
 * Created by luthien on 25/10/2021.
 * Processes a User Removed event caught by EuropeanaEventListenerProviderFactory
 */
public class UserRemovedMessageHandler {

    private static final Logger LOG = Logger.getLogger(UserRemovedMessageHandler.class);

    public static final String CLIENT_OWNER = "client_owner";
    public static final String SHARED_OWNER ="shared_owner";
    public static final int DAYS_TO_ADD = 30;
    private final CloseableHttpClient httpClient;
    private final String prefix;
    private boolean setsDeleteOK       = false;
    private boolean slackHttpMessageOK  = false;
    private boolean slackEmailMessageOK = false;




    private String userSetToken;

    public UserRemovedMessageHandler(String prefix) {
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
        RealmModel realm      = deleteEvent.getRealm();
        UserModel  slackUserModel  = session.users().getUserByUsername(realm, SLACK_USERNAME);

        //Remove the personal apikey associated to the user
        removeKeysAndNotify(session, deleteEvent, realm);

        if (getUserSetToken()){
            setsDeleteOK = sendUserSetDeleteRequest(deleteEvent, userSetToken);
        }
        if (DEBUG_LOGS) {
            LOG.info(formatMessage(deleteEvent, setsDeleteOK ? USER_SETS_DELETED : USER_SETS_NOT_DELETED));
            LOG.info(formatMessage(deleteEvent, SENDING_CONFIRM_MSG_SLACK));
        }
        if (null != SLACK_WEBHOOK && !"".equalsIgnoreCase(SLACK_WEBHOOK)) {
            String message = formatUserRemovedMessage(deleteEvent, true);
            LOG.info("message " + message);
            slackHttpMessageOK = sendSlackHttpMessage(deleteEvent,message,SLACK_WEBHOOK);
        }
        if (!slackHttpMessageOK) {
            if (DEBUG_LOGS) {
                LOG.info(formatMessage(deleteEvent, HTTP_FAILED_TRYING_EMAIL));
            }
             String message = formatUserRemovedMessage(deleteEvent, false);
             slackEmailMessageOK = sendSlackEmailMessage(session, slackUserModel, deleteEvent,message);
        }

        if (slackHttpMessageOK || slackEmailMessageOK) {
            LOG.info(formatMessage(deleteEvent, SLACK_MSG_SENT));
        } else {
            LOG.error(formatMessage(deleteEvent, HTTP_AND_EMAIL_FAILED));
        }
    }

    private void removeKeysAndNotify(KeycloakSession session, UserRemovedEvent deleteEvent,
        RealmModel realm) {
        UserModel user = deleteEvent.getUser();
        List<String> projectkeysWithoutUser = removeKeysAssociatedToUser(session, user,
            realm);
        if(!projectkeysWithoutUser.isEmpty()){
            String msg = String.format(MSG_PROJECT_KEY_WITH_NO_USER,user.getUsername(),user.getEmail(),String.join(",", projectkeysWithoutUser));
            if(!sendSlackHttpMessage(deleteEvent,msg,SLACK_WEBHOOK_ORPHAN_PROJECT_KEY)){
                LOG.error(formatMessage(deleteEvent,"Failed slack message is - "+ msg));
            }
        }
    }
    private static List<String> removeKeysAssociatedToUser(KeycloakSession session, UserModel userModel,
        RealmModel realm) {
        List<RoleModel> rolesAssociatedToUser = userModel.getRoleMappingsStream().filter(
            roleModel -> (CLIENT_OWNER.equals(roleModel.getName()) || SHARED_OWNER.equals(
                roleModel.getName()))).toList();

        List<String> projectkeysWithoutUser = new ArrayList<>();
        for (RoleModel rolemodel : rolesAssociatedToUser) {
            ClientModel client = (ClientModel) rolemodel.getContainer();
            if (rolemodel.isClientRole() && client!=null) {
                    if (CLIENT_OWNER.equals(rolemodel.getName())) {
                        dissociatePrivateKey(session, userModel, realm, client);
                    }
                    if (SHARED_OWNER.equals(rolemodel.getName()) &&
                        //get All users to whom the client(project key) is associated and check if it was the last user associated to that project key
                        session.users().getRoleMembersStream(realm, rolemodel).findAny()
                            .isEmpty()) {
                        projectkeysWithoutUser.add(client.getClientId());
                    }
            }
        }
        return  projectkeysWithoutUser;
    }

    private static void  dissociatePrivateKey(KeycloakSession session, UserModel userModel, RealmModel realm,
        ClientModel clientModel) {
        if (clientModel != null){
           //Remove Private key associated to user
           if (session.clients().removeClient(realm, clientModel.getId())) {
               LOG.info("Removed the private key "+clientModel.getClientId()+" associated to user-" + userModel.getUsername());
           }
        }else {
            LOG.error("Unable to remove private key associated to user");
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
            HttpPost     httpPost = new HttpPost(OID_TOKEN_URL);
            if (DEBUG_LOGS){
                LOG.info("Auth server OID url: " + OID_TOKEN_URL);
            }
            try {
                httpPost.setEntity(new UrlEncodedFormEntity( getPostParameters(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.error(e);
                return false;
            }
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return processResponse(response);
            } catch (Exception e) {
                LOG.error(e);
                return false;
            }
    }

    private boolean processResponse(CloseableHttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            LOG.error("Error sending POST request, received HTTP response code" +
                      response.getStatusLine().getStatusCode() );
            return false;
        } else {
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            userSetToken = readJsonValue(responseString, "access_token");
            if (isNotBlank(userSetToken)){
                if (DEBUG_LOGS){
                    LOG.info("User set access token: " + userSetToken);
                }
                return true;
            } else {
                LOG.error("No access token found in response from Keycloak: " + responseString);
                return false;
            }
        }
    }

    private static ArrayList<NameValuePair> getPostParameters() {
        ArrayList<NameValuePair> postParameters;

        postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
        postParameters.add(new BasicNameValuePair("username", DELETE_MGR_ID));
        postParameters.add(new BasicNameValuePair("password", DELETE_MGR_PW));
        postParameters.add(new BasicNameValuePair("client_id", CLIENT_ID));
        postParameters.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
        postParameters.add(new BasicNameValuePair("scope", SCOPE));
        return postParameters;
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
                LOG.info(formatMessage(deleteEvent,
                                "Usersets delete request successful: received HTTP " +
                                response.getStatusLine().getStatusCode() ));
                return true;
            } else {
                if (DEBUG_LOGS) {
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity);
                    LOG.error("Usersets delete request was not successful: received HTTP " +
                              response.getStatusLine().getStatusCode() +
                              " response. Response body: " + responseBody);
                } else {
                    LOG.error(formatMessage(deleteEvent,
                                 "Usersets delete request was not successful: received HTTP " +
                                 response.getStatusLine().getStatusCode()));
                }
                return false;
            }
        } catch (IOException e) {
            if (DEBUG_LOGS) {
                LOG.error(e);
            } else {
                LOG.error(formatMessage(deleteEvent,
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
                             LocalDate.now().plusDays(DAYS_TO_ADD));
    }

    /**
     * Send message to the Slack channel with a POST HTTP request
     *
     * @param deleteEvent UserRemovedEvent containing the removed user data
     * @return boolean: TRUE  if HTTP 200 was received after sending the message
     *                  FALSE in case of any other status or an error occurring
     */
    private boolean sendSlackHttpMessage(UserRemovedEvent deleteEvent,String message,String slackWebhook) {

        StringEntity entity;


        try {
            entity = new StringEntity(message);
        } catch (UnsupportedEncodingException e) {
            LOG.errorf("UnsupportedEncodingException occurred while creating Slack message", e);
            return false;
        }

        HttpPost     httpPost = new HttpPost(slackWebhook);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOG.info(formatMessage(deleteEvent,
                                "Slack message sent successfully: received HTTP " +
                                response.getStatusLine().getStatusCode() +
                                " response"));
                return true;
            } else {
                LOG.error(formatMessage(deleteEvent,
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
     * @param slackUserModel  Slack user in Keycloak
     * @param deleteEvent UserRemovedEvent containing the removed user data
     * @return boolean TRUE  if the email was sent without an error
     *                 FALSE if an EmailException error occurred
     */
    private boolean sendSlackEmailMessage(KeycloakSession session, UserModel slackUserModel, UserRemovedEvent deleteEvent,String message) {


        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        UserModel deleteUser = deleteEvent.getUser();

        try {
            if(slackUserModel!=null) {
                LOG.info(formatMessage(deleteEvent,
                    "Sending email to Slack user: " + slackUserModel.getEmail()));
                senderProvider.send(
                    session.getContext().getRealm().getSmtpConfig(),
                    slackUserModel,
                    "User account for Keycloak user with ID: " + deleteUser.getId(),
                    message,
                    message);

            return true;
            }
            LOG.info(formatMessage(deleteEvent,
                "Sending email to Slack user failed for : " + deleteUser.getId()));
            return false;
        } catch (EmailException e) {
            LOG.error(formatMessage(deleteEvent,
                             "EmailException occurred while sending Slack message by email: " + e.getMessage()));
            return false;
        }
    }

    private String formatMessage(UserRemovedEvent deleteEvent, String message) {
        StringBuilder msg = new StringBuilder();

        msg.append("type: USER_DELETE_EVENT");

        if (deleteEvent.getRealm() != null) {
            msg.append(", realm: ");
            msg.append(deleteEvent.getRealm().getName());
        }

        if (deleteEvent.getUser() != null) {
            if (isNotBlank(deleteEvent.getUser().getId())) {
                msg.append(", userId: ");
                msg.append(deleteEvent.getUser().getId());
            }
            if (isNotBlank(deleteEvent.getUser().getEmail())) {
                msg.append(", userEmail: ");
                msg.append(deleteEvent.getUser().getEmail());
            }
            if (isNotBlank(deleteEvent.getUser().getUsername())) {
                msg.append(", userName: ");
                msg.append(deleteEvent.getUser().getUsername());
            }
        }

        if (message != null) {
            msg.append(", message: ");
            msg.append(msg);
        }

        msg.append(" ");
        return prefix + msg;
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