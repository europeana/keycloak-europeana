package eu.europeana.keycloak.metrics;

import jakarta.ws.rs.Path;
import java.util.concurrent.atomic.AtomicInteger;
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

    public static final String CLIENT_OWNER = "client_owner";
    public static final String SHARED_OWNER = "shared_owner";
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
    @Produces("application/json; charset=utf-8")
    public String get() {
        //Iterate Over the keycloak clients(i.e apikeys) to generate the count of private and project keys
        AtomicInteger privateKeyCount = new AtomicInteger(0);//Get clients with role client_owner
        AtomicInteger projectKeyCount =new AtomicInteger(0);//Get clients with role shared_owner

        RealmModel realm = session.getContext().getRealm();
        session.clients().getClientsStream(realm).forEach(clientModel -> {
            if (clientModel.getRole(CLIENT_OWNER) != null) {
                privateKeyCount.getAndIncrement();
            }
            if (clientModel.getRole(SHARED_OWNER) != null) {
                projectKeyCount.getAndIncrement();
            }
        });
        return toJson(countUsers(realm),projectKeyCount.get(), privateKeyCount.get());
    }

    private int countUsers(RealmModel realm) {
        return session.users().getUsersCount(realm);
    }

    @Override
    public void close() {
    }


    private String toJson(int nrOfUsers,int nrOfProjectkeys,int nrOfPrivatekeys) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", String.valueOf(nrOfUsers));
        obj.add("NumberOfProjectClients",nrOfProjectkeys);
        obj.add("NumberOfPersonalClients",nrOfPrivatekeys);
        return obj.build().toString();
    }

}
