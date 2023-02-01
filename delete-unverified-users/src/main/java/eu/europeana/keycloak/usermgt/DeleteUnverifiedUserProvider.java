package eu.europeana.keycloak.usermgt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static org.keycloak.utils.StringUtil.isNotBlank;
//import java.time.Instant;

/**
 * Created by luthien on 14/11/2022.
 */
public class DeleteUnverifiedUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(DeleteUnverifiedUserProvider.class);
    private static final String LOG_PREFIX = "KEYCLOAK_EVENT:";

    private static final String SUCCESS_MSG = "User account deleted: email was not verified within 24 hours";

    private static Map<String, String> EMAIL_NOT_VERIFIED;

    static {
        EMAIL_NOT_VERIFIED = new HashMap<>();
        EMAIL_NOT_VERIFIED.put(UserModel.EMAIL_VERIFIED, "false");
    }

    // change this value to set how many hours before {SYSDATE} unverified users (i.e. not confirmed by email)
    // must have registered at least, before they are removed when this add-on is triggered
    // (e.g. when set to 24L => removes all unverified users registered before yesterday, same time)
    private final static Long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;


    private KeycloakSession session;

    private RealmModel realm;

    private UserProvider userProvider;

    public DeleteUnverifiedUserProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.userProvider = session.users();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String delete(
            @DefaultValue("1") @QueryParam("age") int minimumAge) {
        return removeUnverifiedUsers(minimumAge);
    }

    @Override
    public void close() {
    }

    /**
     * This method works by retrieving a Stream of UserModels from the UserProvider filtered on the property
     * UserModel.EMAIL_VERIFIED = "false" and is filtered further by comparing the Created timestamp to the
     * (Sysdate - SO_MANY_HOURS_AGO) Long value defined above in milliseconds
     *
     * @return String with result message (TBD)
     */
    public String removeUnverifiedUsers(int minimumAge) {
        int nrOfDeletedUsers = 0;
        List<UserModel> unverifiedUsersToYesterday = userProvider.searchForUserStream(
                realm,
                EMAIL_NOT_VERIFIED)
                .filter(u -> u.getCreatedTimestamp() < (System.currentTimeMillis() - (MILLIS_PER_DAY * minimumAge)))
                .collect(Collectors.toList());

        for (UserModel user : unverifiedUsersToYesterday) {
            nrOfDeletedUsers++;
            LOG.info(": " + toJson(user, SUCCESS_MSG, nrOfDeletedUsers));
        }

//        SELECT *
//                FROM keycloak20.user_entity
//        WHERE email_verified = FALSE AND service_account_client_link IS NOT NULL

//        for (UserModel user : unverifiedUsersToYesterday) {
//            if (userProvider.removeUser(realm, user)) {
//                nrOfDeletedUsers++;
//                LOG.info(": " + toJson(user, SUCCESS_MSG, nrOfDeletedUsers));
//            }
//        }
        return toJson(null, SUCCESS_MSG, nrOfDeletedUsers);
    }

    private String toJson(UserModel user, String msg, int nrOfDeletedUsers) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "UNVERIFIED_USER_DELETE");

        if (realm != null) {
            obj.add("realmName", realm.getName());
        }

        if (user != null) {
            if (isNotBlank(user.getId())) {
                obj.add("userId", user.getId());
            }
            if (isNotBlank(user.getEmail())) {
                obj.add("userEmail", user.getEmail());
            }
            if (isNotBlank(user.getUsername())) {
                obj.add("userName", user.getUsername());
            }
        }

        if (msg != null) {
            obj.add("message", msg);
        }

        if (nrOfDeletedUsers > 0) {
            obj.add("Number of users deleted", nrOfDeletedUsers);
        }

        return LOG_PREFIX + obj.build().toString();
    }

}
