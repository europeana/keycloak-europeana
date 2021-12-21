package eu.europeana.keycloak.logging;

import static eu.europeana.keycloak.user.UserRemovedConfig.JSONLOGPREFIXENVVAR;
import static eu.europeana.keycloak.user.UserRemovedConfig.LOG_PREFIX;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_USER;
import static eu.europeana.keycloak.user.UserRemovedConfig.SLACK_WEBHOOK;

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

    private static final Logger LOG = Logger.getLogger("org.keycloak.events");

    KeycloakSession session;

    UserRemovedMessageHandler userRemovedMessageHandler;
    private String logPrefix;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        this.session = session;
        return new EuropeanaEventListenerProvider(session, LOG, logPrefix);
    }

    @Override
    public void init(Config.Scope scope) {

        if (null != System.getenv(JSONLOGPREFIXENVVAR)){
            logPrefix = System.getenv(JSONLOGPREFIXENVVAR);
        } else {
            logPrefix = LOG_PREFIX;
        }

        if (null == System.getenv(SLACK_WEBHOOK) && null == System.getenv(SLACK_USER)){
            throw new RuntimeException("Slack webhook nor user environment variables found, exiting ...");
        } else {
            this.userRemovedMessageHandler = new UserRemovedMessageHandler(System.getenv(SLACK_WEBHOOK), System.getenv(SLACK_USER), LOG, logPrefix);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(
            (event) -> {
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
        return "jsonlog_event_listener";
    }
}
