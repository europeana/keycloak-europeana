package eu.europeana.keycloak.logging;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;


public class JSONLogEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger("org.keycloak.events");
    private static final String JSONLOGPREFIXENVVAR = "KEYCLOAK_JSONLOG_PREFIX";

    String prefix = "KEYCLOAK_EVENT:";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new JSONLogEventListenerProvider(session, logger, prefix);
    }

    @Override
    public void init(Config.Scope scope) {
        String envPrefix = System.getenv(JSONLOGPREFIXENVVAR);
        if (envPrefix != null) {
            prefix = envPrefix;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(
            (event) -> {
                //do something here with the UserRemovedEvent
                //the user is available via event.getUser()
                if (event instanceof UserModel.UserRemovedEvent){
                    logger.info("Boom! User removed event just dropped with user: " + ((UserRemovedEvent) event).getUser().getEmail());
                }
            });
    }

    @Override
    public void close() {
        // no need to implement this
    }

    @Override
    public String getId() {
        return "jsonlog_event_listener";
    }
}
