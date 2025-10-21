package eu.europeana.keycloak.metrics;

import eu.europeana.keycloak.repo.CustomClientRepository;
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

import static eu.europeana.keycloak.utils.Constants.SHARED_OWNER;
import static eu.europeana.keycloak.utils.Constants.ROLE_ATTRIBUTE_SCOPE_INTERNAL;
import static eu.europeana.keycloak.utils.Constants.ROLE_ATTRIBUTE_SCOPE;
import static eu.europeana.keycloak.utils.Constants.CLIENT_OWNER;

/**
 * Simple user count endpoint
 */
@Provider
public class UserCountProvider implements RealmResourceProvider {

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
        int internalKeys = countKeysByRoleAndAttribute(SHARED_OWNER, ROLE_ATTRIBUTE_SCOPE, ROLE_ATTRIBUTE_SCOPE_INTERNAL);
        return toJson(countUsers(session.getContext().getRealm()),
                countKeysByRole(SHARED_OWNER),
                (countKeysByRole(CLIENT_OWNER) - internalKeys),
                internalKeys);
    }

    private int countKeysByRole(String roleName){
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CustomClientRepository clientRepo = new CustomClientRepository(entityManager);
        Long clientCount = clientRepo.findKeyByRoleName(roleName);
        return clientCount.intValue();
    }

    private int countKeysByRoleAndAttribute(String roleName, String attributeName, String attributeValue){
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CustomClientRepository clientRepo = new CustomClientRepository(entityManager);
        return clientRepo.findKeyByRoleNameAndAttributePair(roleName, attributeName, attributeValue).intValue();
    }

    private int countUsers(RealmModel realm) {
        return session.users().getUsersCount(realm);
    }

    @Override
    public void close() {
        // do nothing
    }


    /**
     * form a json
     *       {
     *         "created": "2025-10-03T11:51:23.823215506Z",
     *         "NumberOfUsers": 6475,
     *        "RegisteredClients": {
     *                  "Personal": 32,
     *                  "Project": 132,
     *                  "Internal": 3
     *          }
     *      }
     * @param nrOfUsers
     * @param nrOfProjectkeys
     * @param nrOfPrivatekeys
     * @param internalkeys
     * @return
     */
    private String toJson(int nrOfUsers,int nrOfProjectkeys,int nrOfPrivatekeys, int internalkeys) {
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", nrOfUsers);
        obj.add("RegisteredClients",
                   Json.createObjectBuilder()
                           .add("Personal", nrOfPrivatekeys)
                           .add("Project", nrOfProjectkeys)
                           .add("Internal", internalkeys));

        return obj.build().toString();
    }
}