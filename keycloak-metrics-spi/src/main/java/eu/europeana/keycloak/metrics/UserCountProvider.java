package eu.europeana.keycloak.metrics;

import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.time.Instant;

/**
 * globl
 */
public class UserCountProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public UserCountProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces("text/plain; charset=utf-8")
    public String get() {
        return toJson(countUsers(session.getContext().getRealm()));
    }

    private int countUsers(RealmModel realm) {
        return session.users().getUsersCount(realm);
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
