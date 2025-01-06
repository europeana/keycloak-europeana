package eu.europeana.keycloak.sessions;

import eu.europeana.keycloak.sessions.entity.SessionHistory;
import java.util.Arrays;
import java.util.List;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

/**
 * Created by luthien on 19/12/2024.
 */
public class SessionHistoryProvider  implements JpaEntityProvider {

    private static Class<?>[] entities = {SessionHistory.class};

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.<Class<?>>asList(entities);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/jpa-changelog-23.0.0.xml";
    }

    @Override
    public void close() {}

    @Override
    public String getFactoryId() {
        return SessionHistoryProviderFactory.ID;
    }

}
