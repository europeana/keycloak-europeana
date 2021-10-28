package eu.europeana.keycloak.logging;

import eu.europeana.keycloak.user.UserDeleteRequestHandler;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.email.DefaultEmailSenderProvider;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Map;

public class JSONLogEventListenerProvider implements EventListenerProvider {

    UserDeleteRequestHandler userDeleteRequestHandler;
    private final KeycloakSession session;
    private final RealmProvider realmProvider;
    Logger logger;
    String prefix;
    EventReporter eventReporter;

    public JSONLogEventListenerProvider(KeycloakSession session, Logger logger, String prefix) {
        this.session = session;
        this.realmProvider = session.realms();
        this.logger = logger;
        this.prefix = prefix;
        this.userDeleteRequestHandler = new UserDeleteRequestHandler();
        if (null != System.getenv("SLACK_WEBHOOK") && null != System.getenv("SLACK_USER")){
            eventReporter = new EventReporter(System.getenv("SLACK_WEBHOOK"), System.getenv("SLACK_USER"));
        } else if (null == System.getenv("SLACK_USER")){
            eventReporter = new EventReporter(System.getenv("SLACK_WEBHOOK"), true);
        } else if (null == System.getenv("SLACK_WEBHOOK")){
            eventReporter = new EventReporter(System.getenv("SLACK_USER"), false);
        } else {
            throw new RuntimeException("Slack webhook nor user environment variables found, exiting ...");

        }
    }

    @Override
    public void onEvent(Event event) {
        if (EventType.DELETE_ACCOUNT.equals(event.getType())) {
            logger.log(Logger.Level.DEBUG, "PINGGGGG DELETE USER! --> " + toString(event) + "; user " + event.getUserId());

//            RealmModel realm = this.realmProvider.getRealm(event.getRealmId());
//            UserModel  user  = this.session.users().getUserById(event.getUserId(), realm);
//
//            System.out.println("********************[ USER EMAIL: " + user.getEmail() + " ]");
//            eventReporter.sendUserDeletedMessage(session, user);

        }
        String msg = prefix + toString(event);
        logger.log(Logger.Level.INFO, msg);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
//        if (ResourceType.USER.equals(adminEvent.getResourceType()) &&
//            OperationType.DELETE.equals(adminEvent.getOperationType())){
//
//            RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
//
//            UserModel  user  = session.users().getUserById(adminEvent.getResourcePath().substring("users/".length()), realm);
//
//            logger.log(Logger.Level.ERROR, "PINGGGGG DELETE USER! --> " + toString(adminEvent) + "; user: " + user.getId() );
//
//
//            eventReporter.sendUserDeleteMessage(session, adminEvent);
//        }
        String msg = prefix + toString(adminEvent);
        logger.log(Logger.Level.INFO, msg);
    }

    @Override
    public void close() {
        // no need to implement this
    }

    private String toString(Event event) {

        JsonObjectBuilder obj = Json.createObjectBuilder();

        if (event.getType() != null) {
            obj.add("type", event.getType().toString());
        }

        if (event.getRealmId() != null) {
            obj.add("realmId", event.getRealmId());
        }

        if (event.getClientId() != null) {
            obj.add("clientId", event.getClientId());
        }

        if (event.getUserId() != null) {
            obj.add("userId", event.getUserId());
        }

        if (event.getIpAddress() != null) {
            obj.add("ipAddress", event.getIpAddress());
        }


        if (event.getError() != null) {
            obj.add("error", event.getError());
        }

        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                obj.add(e.getKey(), e.getValue());
            }
        }
        return obj.build().toString();
    }


    private String toString(AdminEvent adminEvent) {
        JsonObjectBuilder obj = Json.createObjectBuilder();

        obj.add("type", "ADMIN_EVENT");

        if (adminEvent.getOperationType() != null) {
            obj.add("operationType", adminEvent.getOperationType().toString());
        }

        if (adminEvent.getAuthDetails() != null) {
            if (adminEvent.getAuthDetails().getRealmId() != null) {
                obj.add("realmId", adminEvent.getAuthDetails().getRealmId());
            }

            if (adminEvent.getAuthDetails().getClientId() != null) {
                obj.add("clientId", adminEvent.getAuthDetails().getClientId());
            }

            if (adminEvent.getAuthDetails().getUserId() != null) {
                obj.add("userId", adminEvent.getAuthDetails().getUserId());
            }

            if (adminEvent.getAuthDetails().getIpAddress() != null) {
                obj.add("ipAddress", adminEvent.getAuthDetails().getIpAddress());
            }

        }

        if (adminEvent.getResourceType() != null) {
            obj.add("resourceType", adminEvent.getResourceType().toString());
        }

        if (adminEvent.getResourcePath() != null) {
            obj.add("resourcePath", adminEvent.getResourcePath());
        }

        if (adminEvent.getError() != null) {
            obj.add("error", adminEvent.getError());
        }

        return obj.build().toString();
    }

}
