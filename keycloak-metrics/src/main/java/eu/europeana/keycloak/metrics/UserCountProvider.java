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

    /** Constructs new UserCountProvider with the provided keycloak session
     * @param session current keycloak session
     */
    public UserCountProvider(KeycloakSession session) {this.session = session;}

    @Override
    public Object getResource() {
        return this;
    }

    /** Fetches the count of users and clients by type
     * @return json string
     */
    @Path("")
    @GET
    @Produces("application/json; charset=utf-8")
    public String get() {
        return toJson(countUsers(session.getContext().getRealm()),
            countKeysByRole(SHARED_OWNER),
            countKeysByRole(CLIENT_OWNER),
            0);
    }

    private int countKeysByRole(String roleName){
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CustomClientRepository clientRepo = new CustomClientRepository(entityManager);
        Long clientCount = clientRepo.findKeyByRoleName(roleName);
        return clientCount.intValue();
    }

//    private int countKeysByRoleAttribute(String roleName, String attname, String value){
//        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
//        CustomClientRepository clientRepo = new CustomClientRepository(entityManager);
//        //Long clientCount = clientRepo.findKeyByRoleName1(roleName, attname, value);
//        return clientCount.intValue();
//    }

    private int countUsers(RealmModel realm) {
        return session.users().getUsersCount(realm);
    }

    @Override
    public void close() {
        // do nothing
    }


    private String toJson(int nrOfUsers,int nrOfProjectkeys,int nrOfPrivatekeys, int internalkeys) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", nrOfUsers);
        obj.add("NumberOfProjectClients",nrOfProjectkeys);
        obj.add("NumberOfPersonalClients",nrOfPrivatekeys);
        obj.add("Internal",internalkeys);

        return obj.build().toString();
    }
}