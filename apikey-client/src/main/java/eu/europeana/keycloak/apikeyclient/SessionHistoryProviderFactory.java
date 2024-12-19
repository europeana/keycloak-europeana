package eu.europeana.keycloak.apikeyclient;

import eu.europeana.keycloak.apikeyclient.SessionHistoryProvider;
import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Created by luthien on 19/12/2024.
 */

public class SessionHistoryProviderFactory implements JpaEntityProviderFactory {

    public static final String ID = "session-history-entity-provider";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new SessionHistoryProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {}

}


