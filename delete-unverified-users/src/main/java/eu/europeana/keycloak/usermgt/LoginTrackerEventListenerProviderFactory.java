package eu.europeana.keycloak.usermgt;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class LoginTrackerEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "login-tracker-listener";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LoginTrackerEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No custom config needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-init needed
    }

    @Override
    public void close() {
        // No factory resource cleanup needed
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
