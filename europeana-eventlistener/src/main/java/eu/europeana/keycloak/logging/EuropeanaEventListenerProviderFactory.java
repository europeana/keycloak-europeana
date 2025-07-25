package eu.europeana.keycloak.logging;

import static eu.europeana.keycloak.user.UserRemovedConfig.LOG_PREFIX;

import eu.europeana.keycloak.user.UserRemovedMessageHandler;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;


public class EuropeanaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(EuropeanaEventListenerProviderFactory.class);

    KeycloakSession session;

    UserRemovedMessageHandler userRemovedMessageHandler;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        this.session = session;
        return new EuropeanaEventListenerProvider(LOG, LOG_PREFIX);
    }

    @Override
    public void init(Config.Scope scope) {
        this.userRemovedMessageHandler = new UserRemovedMessageHandler(LOG_PREFIX);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(
            event -> {
                //do something here with the UserRemovedEvent
                //the user is available via event.getUser()
                if (event instanceof UserModel.UserRemovedEvent){
                    userRemovedMessageHandler.handleUserRemoveEvent(session, (UserRemovedEvent) event);
                    LOG.info("User removed event happened for user: " + ((UserRemovedEvent) event).getUser().getEmail());
                }
            });
    }

    @Override
    public void close() {
        // no need to implement this
    }

    @Override
    public String getId() {
        return "europeana-eventlistener";
    }
}