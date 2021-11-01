package eu.europeana.keycloak.logging;

import static eu.europeana.keycloak.user.UserDeleteConfig.JSONLOGPREFIXENVVAR;
import static eu.europeana.keycloak.user.UserDeleteConfig.LOG_PREFIX;
import static eu.europeana.keycloak.user.UserDeleteConfig.SLACK_USER;
import static eu.europeana.keycloak.user.UserDeleteConfig.SLACK_WEBHOOK;

import eu.europeana.keycloak.user.UserDeleteEmailHandler;
import eu.europeana.keycloak.user.UserDeleteHttpHandler;
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

    UserDeleteEmailHandler userDeleteEmailHandler;
    UserDeleteHttpHandler userDeleteHttpHandler;
    private String logPrefix;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        this.session = session;
        return new EuropeanaEventListenerProvider(session, LOG, logPrefix);
    }

    @Override
    public void init(Config.Scope scope) {

        this.userDeleteHttpHandler = new UserDeleteHttpHandler();

        if (null != System.getenv(SLACK_WEBHOOK) && null != System.getenv(SLACK_USER)){
            this.userDeleteEmailHandler = new UserDeleteEmailHandler(System.getenv(SLACK_WEBHOOK), System.getenv(SLACK_USER));
        } else if (null == System.getenv(SLACK_USER)){
            this.userDeleteEmailHandler = new UserDeleteEmailHandler(System.getenv(SLACK_WEBHOOK), true);
        } else if (null == System.getenv(SLACK_WEBHOOK)){
            this.userDeleteEmailHandler = new UserDeleteEmailHandler(System.getenv(SLACK_USER), false);
        } else {
            throw new RuntimeException("Slack webhook nor user environment variables found, exiting ...");
        }

        if (null != System.getenv(JSONLOGPREFIXENVVAR)){
            logPrefix = System.getenv(JSONLOGPREFIXENVVAR);
        } else {
            logPrefix = LOG_PREFIX;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        keycloakSessionFactory.register(
            (event) -> {
                //do something here with the UserRemovedEvent
                //the user is available via event.getUser()
                if (event instanceof UserModel.UserRemovedEvent){
                    userDeleteEmailHandler.sendUserDeletedMessage(session, (UserRemovedEvent) event);
                    LOG.info("Boom! User removed event just dropped with user: " + ((UserRemovedEvent) event).getUser().getEmail());
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
