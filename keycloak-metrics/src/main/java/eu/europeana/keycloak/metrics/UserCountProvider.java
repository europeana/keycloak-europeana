package eu.europeana.keycloak.metrics;

import jakarta.ws.rs.Path;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;

/**
 * Simple user count endpoint
 */
@Provider
public class UserCountProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public UserCountProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Path("")
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
