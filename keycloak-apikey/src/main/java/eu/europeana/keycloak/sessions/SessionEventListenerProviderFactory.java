package eu.europeana.keycloak.sessions;

import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Created by luthien on 19/12/2024.
 */

public class SessionEventListenerProviderFactory implements InfinispanConnectionProviderFactory {

    private static final Logger LOG = Logger.getLogger(SessionEventListenerProviderFactory.class);

    @Override
    public SessionEventListenerProvider create(KeycloakSession keycloakSession) {
        return new SessionEventListenerProvider(keycloakSession, LOG);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "session-event-listener";
    }

    @Override
    public int order() {
        return InfinispanConnectionProviderFactory.super.order();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return InfinispanConnectionProviderFactory.super.getConfigMetadata();
    }

}


