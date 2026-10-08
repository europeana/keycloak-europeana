package eu.europeana.keycloak.usermgt;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class LoginTrackerEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;

    public LoginTrackerEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        // Only trigger on successful LOGIN events
        if (event.getType() == EventType.LOGIN) {
            RealmModel realm = session.getContext().getRealm();
            if (realm == null || event.getUserId() == null) {
                return;
            }

            UserModel user = session.users().getUserById(realm, event.getUserId());

            // Set attribute once on first successful login
            if (user != null && user.getFirstAttribute("hasLoggedIn") == null) {
                user.setSingleAttribute("hasLoggedIn", "true");
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Not tracking admin actions
    }

    @Override
    public void close() {
        // Nothing to close per-request
    }
}
