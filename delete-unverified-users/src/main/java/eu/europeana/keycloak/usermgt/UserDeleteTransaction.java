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
        LOG.info("## User delete transaction ##");
        LOG.info("-----------------------------------------------------------");
        LOG.info(this.userUuidDto.toString());
        LOG.info("-----------------------------------------------------------");

        try {
            boolean userRemoved = userProvider.removeUser(realm, user);
            if (userRemoved){
                LOG.info("User " + user.getUsername() + ", email " + user.getEmail() + " has been removed");
            } else {
                LOG.error("User" + user.getUsername() + ", email " + user.getEmail() + " could NOT be removed");
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
