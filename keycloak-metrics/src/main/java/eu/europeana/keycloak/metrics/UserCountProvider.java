package eu.europeana.keycloak.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.time.Instant;

/**
 * returns number of users only
 */
public class UserCountProvider implements RealmResourceProvider {

    private KeycloakSession session;
    private UserProvider userProvider;
    private RealmModel realm;

    private static Map<String, String> EMAIL_VERIFIED;

    static {
        EMAIL_VERIFIED = new HashMap<>();
        EMAIL_VERIFIED.put(UserModel.EMAIL_VERIFIED, "true");
    }

    public UserCountProvider(KeycloakSession session) {
        this.session = session;
        this.userProvider = session.users();
        this.realm        = session.getContext().getRealm();
    }

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Called without parameters or with ?unverified=true: returns total number of users
     * Called with ?unverified=false: returns the number of users that have validated their email account
     *
     * @return JSON String
     */
    @GET
    @Produces("text/plain; charset=utf-8")
    public String get(
        @DefaultValue("true") @QueryParam("unverified") boolean includeUnverified) {
        if (includeUnverified){
            return toJson(session.users().getUsersCount(realm));
        } else {
            return toJson(userProvider.searchForUserStream(
                                   realm,
                                   EMAIL_VERIFIED)
                               .collect(Collectors.toList())
                               .size());
        }
    }

    @Override
    public void close() {
    }

    private String toJson(int nrOfUsers) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", String.valueOf(nrOfUsers));

        return obj.build().toString();
    }

}
