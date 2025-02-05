package eu.europeana.keycloak.sessions;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.models.KeycloakSession;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 * Created by luthien on 29/01/2025.
 * also check org.keycloak.models.session
 */
public class SessionEventListenerProvider implements InfinispanConnectionProvider {

    Logger LOG;
    private final KeycloakSession keycloakSession;

    public SessionEventListenerProvider(KeycloakSession keycloakSession, Logger LOG) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public void close() {
    }

    @Override
    public <K, V> Cache<K, V> getCache(String s, boolean b) {
        return null;
    }

    @Override
    public <K, V> RemoteCache<K, V> getRemoteCache(String s) {
        return null;
    }

    @Override
    public TopologyInfo getTopologyInfo() {
        return null;
    }

    @Override
    public CompletionStage<Void> migrateToProtoStream() {
        return null;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor() {
        return null;
    }

    @Override
    public BlockingManager getBlockingManager() {
        return null;
    }
}
