package eu.europeana.keycloak.user;

import static eu.europeana.keycloak.user.UserDeleteConfig.ERROR_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.OK_ICON;
import static eu.europeana.keycloak.user.UserDeleteConfig.SLACK_USER_DELETE_MESSAGEBODY;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;

/**
 * Created by luthien on 25/10/2021.
 */
public class UserDeleteEmailHandler {

    private static final Logger LOG = Logger.getLogger(UserDeleteEmailHandler.class);


    private String slackWebHook;
    private String slackUser;


    public UserDeleteEmailHandler(String slackWebHook, String slackUser) {
        this.slackWebHook = slackWebHook;
        this.slackUser    = slackUser;
    }

    public UserDeleteEmailHandler(String slackId, boolean isWebHook) {
        if (isWebHook) {
            this.slackWebHook = slackId;
        } else {
            this.slackUser = slackId;
        }
    }

    protected void sendUserDeleteMessage(KeycloakSession session, AdminEvent adminEvent) {

        //, );
        RealmModel realm     = session.realms().getRealm(adminEvent.getRealmId());
        UserModel  slackUser = session.users().getUserByUsername(this.slackUser, realm);
        UserModel  user      = session.users().getUserById(adminEvent.getResourcePath().substring("users/".length()),
                                                           realm);

        if (user.getEmail() == null) {
            LOG.warnf("Could not send welcome email due to missing email. realm=%s user=%s", realm.getId(),
                      user.getUsername());
            return;
        }

        UriBuilder authUriBuilder = UriBuilder.fromUri(session.getContext().getUri().getBaseUri());

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("baseUri", authUriBuilder.replacePath("/auth").build());
        mailBodyAttributes.put("username", user.getUsername());

        String       realmName     = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);

        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("welcomeEmailSubject", subjectParams, "welcome-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            LOG.errorf(eex, "Failed to send welcome email. realm=%s user=%s", realm.getId(), user.getUsername());
        }

    }

    public void sendUserDeletedMessage(KeycloakSession session, UserRemovedEvent deleteEvent) {
        RealmModel realm     = deleteEvent.getRealm();
        UserModel  slackUser = session.users().getUserByUsername(this.slackUser, realm);
        UserModel deleteUser = deleteEvent.getUser();
        LOG.info("sendUserDeleteMessage for: " + deleteUser.getUsername() + ", email: " + deleteUser.getEmail());

        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);
        try {
            LOG.info("sending email to: " + slackUser.getEmail());
            senderProvider.send(
                session.getContext().getRealm().getSmtpConfig(),
                slackUser,
                "test",
                "body test, deleted user: " + deleteUser.getUsername(),
                "html test");
        } catch (EmailException e) {
            e.printStackTrace();
        }

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
