package eu.europeana.keycloak.user;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

public class UserRemovedEventListenerProvider implements EventListenerProvider {

    KeycloakSession session;
    private static final Logger LOG = Logger.getLogger(UserRemovedEventListenerProvider.class);
    String prefix;

    public UserRemovedEventListenerProvider(KeycloakSession session, String prefix) {
        this.session = session;
        this.prefix = prefix;
    }

    @Override
    public void onEvent(Event event) {
        System.out.println("Pingggg: " + event.getType());
        if (!EventType.DELETE_ACCOUNT.equals(event.getType())) {
            return;
        }
        KeycloakContext keycloakContext = session.getContext();
//        RealmModel realm = keycloakContext.getRealm();
        UserModel  user  = keycloakContext.getAuthenticationSession().getAuthenticatedUser();

        LOG.errorv("Event caught in {} module when deleting user {}", prefix, user.getId());
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
//        String msg = prefix + toString(adminEvent);
//        LOG.info(msg);
    }

    @Override
    public void close() {
        // no need to implement this
    }

}
