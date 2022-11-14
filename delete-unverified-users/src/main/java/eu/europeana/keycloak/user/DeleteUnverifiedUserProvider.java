package eu.europeana.keycloak.user;

import java.time.Instant;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.resource.RealmResourceProvider;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
//import java.time.Instant;

/**
 * Created by luthien on 14/11/2022.
 */
public class DeleteUnverifiedUserProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public DeleteUnverifiedUserProvider(KeycloakSession session) {
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





    public boolean removeUser(RealmModel realm, UserModel user) {
        return removeUser(realm, user, session.users());
    }

    public boolean removeUser(RealmModel realm, UserModel user, UserProvider userProvider) {
        if (userProvider.removeUser(realm, user)) {
            session.getKeycloakSessionFactory().publish(new UserModel.UserRemovedEvent() {

                @Override
                public RealmModel getRealm() {
                    return realm;
                }

                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }

            });
            return true;
        }
        return false;
    }


    private String toJson(int nrOfUsers) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "OverallTotal");
        obj.add("created", Instant.now().toString());
        obj.add("NumberOfUsers", String.valueOf(nrOfUsers));

        return obj.build().toString();
    }

}
