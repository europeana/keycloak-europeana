package eu.europeana.keycloak.usermgt;

import static eu.europeana.keycloak.usermgt.UserDeleteTransaction.disclaimer;
import static org.keycloak.utils.StringUtil.isNotBlank;

import jakarta.ws.rs.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;
import eu.europeana.keycloak.SlackConnection;

/**
 * Created by luthien on 14/11/2022.
 */
@Provider
public class DeleteUnverifiedUserProvider implements RealmResourceProvider {

    private static final Logger LOG         = Logger.getLogger(DeleteUnverifiedUserProvider.class);
    private static final String LOG_PREFIX  = "KEYCLOAK_EVENT:";
    private static final String SUCCESS_MSG = " unverified user accounts are scheduled for removal because their email addresses were not verified within ";
    private static final String DISCLAIMER = ". (Disclaimer: this method is for testing this addon ONLY and will be used only with the developer's own testing accounts. Hence this disclaimer. There is, therefore, no need to invoke privacy laws regarding the disclosure of personal data or to alert some of the more confrontational members of our network. Thank you.)";

    private static final String DELETION_REPORT_MESSAGE  = "{\"text\":\" %s unverified accounts were deleted.\"}";

    private static final Map<String, String> emailNotVerified;

    static {
        emailNotVerified = new HashMap<>();
        emailNotVerified.put(UserModel.EMAIL_VERIFIED, "false");
        emailNotVerified.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
    }

    // change this value to set how many hours before {SYSDATE} unverified users (i.e. not confirmed by email)
    // must have registered at least, before they are removed when this add-on is triggered
    // (e.g. when set to 24L => removes all unverified users registered before yesterday, same time)
    private  static final Long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserProvider userProvider;

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
     * Removes Users based on these criteria:
     * - UserModel.EMAIL_VERIFIED = "false"
     * - UserModel.INCLUDE_SERVICE_ACCOUNT = "false"
     * - Was created between [minimumAgeInDays] and [maximumAgeInDays] days ago
     * - Has the explicit required action 'VERIFY_EMAIL'
     *
     * @param minimumAgeInDays minimum age in days (default 1)
     * @param maximumAgeInDays maximum age in days (default 14)
     * @return String (completed message)
     */
    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String delete(
            @DefaultValue("1") @QueryParam("age") int minimumAgeInDays,
            @DefaultValue("3") @QueryParam("maxAge") int maximumAgeInDays) {
        return removeUnverifiedUsers(minimumAgeInDays, maximumAgeInDays);
    }

    @Override
    public void close() {
        // No specific implementation required
    }

    private String removeUnverifiedUsers(int minimumAgeInDays, int maximumAgeInDays) {
        int nrOfDeletedUsers = 0;
        List<UserModel> unverifiedUsersToYesterday = getUnverifiedUsers(minimumAgeInDays, maximumAgeInDays);

        for (UserModel user : unverifiedUsersToYesterday) {
            UserDeleteTransaction userDeleteTransaction = new UserDeleteTransaction(userProvider, realm, user);
            session.getTransactionManager().enlistPrepare(userDeleteTransaction);
            nrOfDeletedUsers++;

            LOG.info("#" + nrOfDeletedUsers + " - " + user.getUsername() + " scheduled for deletion");
        }
        if (nrOfDeletedUsers > 0) {
            LOG.info(nrOfDeletedUsers + SUCCESS_MSG + minimumAgeInDays + " to " + maximumAgeInDays + " day(s)");
        } else {
            LOG.info("No unverified users found in the realm " + realm);
        }
        SlackConnection conn = new SlackConnection("SLACK_WEBHOOK_DELETE_UNVERIFIED_USERS");
        conn.publishStatusReport(String.format(DELETION_REPORT_MESSAGE, nrOfDeletedUsers));
        return "Unverified user delete job finished.";
    }

    /**
     * Retrieves UserModels where EMAIL_VERIFIED is false and SERVICE_ACCOUNT is false,
     * bounded within the creation age window [minimumAgeInDays, maximumAgeInDays],
     * and containing the 'VERIFY_EMAIL' required action.
     */
    private List<UserModel> getUnverifiedUsers(int minimumAgeInDays, int maximumAgeInDays) {
        long now = System.currentTimeMillis();
        long minAgeTimestamp = now - (MILLIS_PER_DAY * minimumAgeInDays);
        long maxAgeTimestamp = now - (MILLIS_PER_DAY * maximumAgeInDays);

        return userProvider.searchForUserStream(realm, emailNotVerified)
                .filter(u -> {
                    long created = u.getCreatedTimestamp();

                    // Check 1: Account must be created inside the window [maxAgeTimestamp, minAgeTimestamp]
                    boolean isWithinAgeWindow = created <= minAgeTimestamp && created >= maxAgeTimestamp;

                    // Check 2: Account must explicitly have the VERIFY_EMAIL required action
                    boolean hasVerifyEmailAction = u.getRequiredActionsStream()
                            .anyMatch(UserModel.RequiredAction.VERIFY_EMAIL.name()::equals);

                    return isWithinAgeWindow && hasVerifyEmailAction;
                })
                .toList();
    }

    /**
    * Test method that lists all user accounts that have been created and yet not been verified.
    */
    private String listUnverifiedUsers(int minimumAgeInDays, int maximumAgeInDays) {
        List<UserModel> lazyUsers   = getUnverifiedUsers(minimumAgeInDays, maximumAgeInDays);
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
            lazyList.append("found the effort of validating their email address beyond their capabilities and were " +
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
            lazyList.append(DISCLAIMER);








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
}