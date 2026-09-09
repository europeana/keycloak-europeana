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
    private final UserModel  user;
    private final RealmModel realm;
    static String disclaimer = ". (Disclaimer: this method is for testing this addon ONLY and will be used only with the developer's own testing accounts. Hence this disclaimer. There is, therefore, no need to invoke privacy laws regarding the disclosure of personal data or to alert some of the more confrontational members of our network. Thank you.";

    public UserDeleteTransaction(UserProvider userProvider, RealmModel realm, UserModel user) {

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