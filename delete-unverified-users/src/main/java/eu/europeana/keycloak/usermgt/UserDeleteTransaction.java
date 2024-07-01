package eu.europeana.keycloak.usermgt;

import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

/**
 * Created by luthien on 22/05/2024.
 */
public class UserDeleteTransaction extends AbstractKeycloakTransaction {

    private static final Logger LOG = Logger.getLogger(UserDeleteTransaction.class);

    private final UserProvider userProvider;
    private final UserUuidDto userUuidDto;
    private final UserModel  user;
    private final RealmModel realm;

    public UserDeleteTransaction(UserProvider userProvider, RealmModel realm, UserModel user, UserUuidDto userUuidDto) {
        this.userUuidDto  = userUuidDto;
        this.realm        = realm;
        this.user         = user;
        this.userProvider = userProvider;
    }

    @Override
    protected void commitImpl() {
        try {
            boolean userRemoved = userProvider.removeUser(realm, user);
            if (userRemoved){
                LOG.info(user.getUsername() + " | " + user.getEmail() + " removed");
            } else {
                LOG.error(user.getUsername() + " | " + user.getEmail() + " NOT removed");
            }
        } catch (Exception e) {
            throw new RuntimeException("## User delete transaction failed! ##", e);
        }
    }

    @Override
    protected void rollbackImpl() {
        //
    }

}
