package eu.europeana.keycloak.usermgt;

import static org.keycloak.utils.StringUtil.isNotBlank;

import jakarta.ws.rs.Path;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Created by luthien on 14/11/2022.
 */
@Provider
public class DeleteUnverifiedUserProvider implements RealmResourceProvider {

    private static final Logger LOG         = Logger.getLogger(DeleteUnverifiedUserProvider.class);
    private static final String LOG_PREFIX  = "KEYCLOAK_EVENT:";
    private static final String SUCCESS_MSG = " unverified user accounts are scheduled for removal because their email addresses were not verified within ";
    private static final String DELETION_REPORT_MESSAGE  = "{\"text\":\" %s unverified accounts were deleted.\"}";

    private static Map<String, String> emailNotVerified;

    static {
        emailNotVerified = new HashMap<>();
        emailNotVerified.put(UserModel.EMAIL_VERIFIED, "false");
        emailNotVerified.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
    }

    // change this value to set how many hours before {SYSDATE} unverified users (i.e. not confirmed by email)
    // must have registered at least, before they are removed when this add-on is triggered
    // (e.g. when set to 24L => removes all unverified users registered before yesterday, same time)
    private  static final Long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private KeycloakSession session;

    private RealmModel realm;

    private UserProvider userProvider;

    public DeleteUnverifiedUserProvider(KeycloakSession session) {
        this.session      = session;
        this.realm        = session.getContext().getRealm();
        this.userProvider = session.users();
    }

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Removes Users based on these criteria: - UserModel.EMAIL_VERIFIED = "false" - UserModel.INCLUDE_SERVICE_ACCOUNT =
     * "false" - was created less than [minimumAgeInDays] day(s) ago Details about the number and IDs of deleted users
     * are logged.
     * @param minimumAgeInDays minimum age in days
     * @return String (completed message)
     */
    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String delete(
        @DefaultValue("1") @QueryParam("age") int minimumAgeInDays) {
        return removeUnverifiedUsers(minimumAgeInDays);
    }

    @Override
    public void close() {
        //
    }

    private String removeUnverifiedUsers(int minimumAgeInDays) {
        int             nrOfDeletedUsers           = 0;
        List<UserModel> unverifiedUsersToYesterday = getUnverifiedUsers(minimumAgeInDays);

        for (UserModel user : unverifiedUsersToYesterday) {

             UserDeleteTransaction userDeleteTransaction = new UserDeleteTransaction(userProvider, realm, user);
            session.getTransactionManager().enlistPrepare(userDeleteTransaction);
            nrOfDeletedUsers++;

            LOG.info("#" + nrOfDeletedUsers + " - " + user.getUsername() + " scheduled for deletion");
        }
        if (nrOfDeletedUsers > 0) {
            LOG.info(nrOfDeletedUsers + SUCCESS_MSG + minimumAgeInDays + " day(s)");
        } else {
            LOG.info("No unverified users found in the realm "+realm);
        }
        publishStatusReport(String.format(DELETION_REPORT_MESSAGE,nrOfDeletedUsers));
        return "Unverified user delete job finished.";
    }

    /**
     * This method retrieves a List of UserModels filtered on the property (UserModel.EMAIL_VERIFIED: "false") and
     * excludes all Service Accounts: (UserModel.INCLUDE_SERVICE_ACCOUNT, "false") and also excludes any account created
     * less than [minimumAgeInDays] ago
     *
     * @return List of UserModels
     */
    private List<UserModel> getUnverifiedUsers(int minimumAgeInDays) {
        return userProvider.searchForUserStream(
                               realm,
                emailNotVerified)
                           .filter(u -> u.getCreatedTimestamp() <
                                        (System.currentTimeMillis() - (MILLIS_PER_DAY * minimumAgeInDays)))
                           .toList();
    }

    private String listUnverifiedUsers(int minimumAgeInDays) {
        List<UserModel> lazyUsers   = getUnverifiedUsers(minimumAgeInDays);
        StringBuilder   lazyList    = new StringBuilder();
        int             lazyCounter = 0;
        int             lazySize    = lazyUsers.size();
        if (lazySize == 0) {
            lazyList.append("Hurray, only motivated users today!");
        } else {
            if (lazySize == 1) {
                lazyList.append(lazySize);
                lazyList.append(" user ");
            } else {
                lazyList.append(lazySize);
                lazyList.append(" users ");
            }
            lazyList.append(" found the effort of validating their email address beyond their capabilities and were " +
                            "therefore asked to leave the premises. ");
            if (lazySize > 1) {
                lazyList.append("They are: ");
            } else {
                lazyList.append("He or she is: ");
            }
            for (UserModel lazyUser : lazyUsers) {
                lazyCounter++;
                lazyList.append(lazyUser.getFirstName().charAt(0));
                lazyList.append(". ");
                lazyList.append(lazyUser.getLastName());
                if (lazySize == (lazyCounter + 1)) {
                    lazyList.append(" and ");
                } else if (lazySize > lazyCounter) {
                    lazyList.append(", ");
                }
            }
            lazyList.append(
                ". (Disclaimer: this is just for testing and will be used only on the developer's own testing " +
                "accounts. Invoking the privacy laws for communicating private data is therefore not required. Thank you.");
        }
        LOG.info(lazyList.toString());
        return lazyList.toString();
    }


    private String logMessage(UserModel user, String message, int nrOfDeletedUsers) {
        StringBuilder msg = new StringBuilder();

        msg.append("type: UNVERIFIED_USER_DELETE");

        if (realm != null) {
            msg.append(", realm: ");
            msg.append(realm.getName());
        }

        if (user != null) {
            if (isNotBlank(user.getId())) {
                msg.append(", userId: ");
                msg.append(user.getId());
            }
            if (isNotBlank(user.getEmail())) {
                msg.append(", userEmail: ");
                msg.append(user.getEmail());
            }
            msg.append(", userName: ");
            msg.append(user.getUsername());
        }

        if (message != null) {
            msg.append(", message: ");
            msg.append(msg);
        }

        if (nrOfDeletedUsers > 0) {
            msg.append(". Number of users deleted: ");
            msg.append(nrOfDeletedUsers);
        }

        msg.append(" ");
        return LOG_PREFIX + msg;
    }


    /**
     * Sends the message to configured slack channel.
     * Currently the messages are sent to
     * @param message -
     */
    private void publishStatusReport(String message) {
        LOG.info("Sending Slack Message : " + message);
        try {
            String slackWebhookApiAutomation = System.getenv("SLACK_WEBHOOK_DELETE_UNVERIFIED_USERS");
            if (StringUtils.isBlank(slackWebhookApiAutomation)) {
                LOG.error("Slack webhook not configured, status report will not be published over Slack.");
                return;
            }
            HttpPost httpPost = new HttpPost(slackWebhookApiAutomation);
            StringEntity entity   = new StringEntity(message);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpPost)) {
                LOG.info("Received status " + response.getStatusLine().getStatusCode()
                    + " while calling slack!");
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    LOG.info(" Successfully sent slack message !");
                }
            }
        } catch (IOException e) {
            LOG.error("Exception occurred while sending slack message !! " + e.getMessage());
        }
    }

}