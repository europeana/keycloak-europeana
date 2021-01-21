package eu.europeana.keycloak.user;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;


public class UserRemovedEventListenerProviderFactory implements EventListenerProviderFactory {


//    private static final Logger LOG = Logger.getLogger(UserRemovedEventListenerProviderFactory.class);
    private static final String MODULENAME                 = "UserRemoved";


    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserRemovedEventListenerProvider(session, MODULENAME);
    }

    @Override
    public void init(Config.Scope scope) {
//        String envPrefix = System.getenv(JSONLOGPREFIXENVVAR);
//        if (envPrefix != null) {
//            prefix = envPrefix;
//        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // no need to implement this
    }

    @Override
    public void close() {
        // no need to implement this
    }

    @Override
    public String getId() {
        return "user-removed-eventlistener";
    }
}
