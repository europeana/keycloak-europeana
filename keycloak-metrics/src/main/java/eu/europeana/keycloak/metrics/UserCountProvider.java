package eu.europeana.keycloak.metrics;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Path;
import org.keycloak.connections.jpa.JpaConnectionProvider;
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
    private final KeycloakSession session;
    public UserCountProvider(KeycloakSession session) {this.session = session;}
    @Override
    public Object getResource() {
        return this;
    }

    @Path("")
    @GET
    @Produces("application/json; charset=utf-8")
    public String get() {
        return toJson(countUsers(session.getContext().getRealm()),
            countKeysByRole(SHARED_OWNER),
            countKeysByRole(CLIENT_OWNER));
    }

    private int countKeysByRole(String roleName){
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CustomClientRepository clientRepo = new CustomClientRepository(entityManager);
        Long clientCount = clientRepo.findKeyByRoleName(roleName);
        return clientCount.intValue();
    }

    private int countUsers(RealmModel realm) {
        return session.users().getUsersCount(realm);
    }

    @Override
    public void close() {
        // do nothing
    }


    private String toJson(int nrOfUsers,int nrOfProjectkeys,int nrOfPrivatekeys) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", nrOfUsers);
        obj.add("NumberOfProjectClients",nrOfProjectkeys);
        obj.add("NumberOfPersonalClients",nrOfPrivatekeys);
        return obj.build().toString();
    }

}
