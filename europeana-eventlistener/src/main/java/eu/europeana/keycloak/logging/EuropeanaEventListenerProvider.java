package eu.europeana.keycloak.logging;

import java.util.Map;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public class EuropeanaEventListenerProvider implements EventListenerProvider {


    private final String prefix;
    Logger LOG;

    public EuropeanaEventListenerProvider(KeycloakSession session, Logger LOG, String prefix) {
        this.LOG    = LOG;
        this.prefix = prefix;
    }

    @Override
    public void onEvent(Event event) {
        String msg = prefix + formatEventLog(event);
        LOG.info(msg);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        String msg = prefix + formatEventLog(adminEvent);
        LOG.info(msg);
    }

    @Override
    public void close() {
        // no need to implement this
    }

    private String formatEventLog(Event event) {
        StringBuilder eventLog = new StringBuilder();
        boolean isNotFirst = false;

        if (event.getType() != null) {
            eventLog.append("type: ");
            eventLog.append(event.getType().toString());
            isNotFirst = true;
        }

        if (event.getRealmId() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            eventLog.append("realm: ");
            eventLog.append(event.getRealmId());
            isNotFirst = true;
        }

        if (event.getClientId() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            eventLog.append("clientId: ");
            eventLog.append(event.getClientId());
            isNotFirst = true;
        }

        if (event.getUserId() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            eventLog.append("userId: ");
            eventLog.append(event.getUserId());
            isNotFirst = true;
        }

        if (event.getIpAddress() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            eventLog.append("ipAddress: ");
            eventLog.append(event.getIpAddress());
            isNotFirst = true;
        }

        if (event.getError() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            eventLog.append("error: ");
            eventLog.append(event.getError());
            isNotFirst = true;
        }

        if (event.getDetails() != null) {
            if (isNotFirst){
                eventLog.append(", ");
            }
            boolean isNotFirstEither = false;
            eventLog.append("details: ");
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                if (isNotFirstEither){
                    eventLog.append(", ");
                }
                eventLog.append(e.getKey());
                eventLog.append(": ");
                eventLog.append(e.getValue());
                isNotFirstEither = true;
            }
        }
        eventLog.append(" ");
        return eventLog.toString();
    }


    private String formatEventLog(AdminEvent adminEvent) {
        StringBuilder adminEventLog = new StringBuilder();
        adminEventLog.append("type: ADMIN_EVENT");


        if (adminEvent.getOperationType() != null) {
            adminEventLog.append(", operationType: ");
            adminEventLog.append(adminEvent.getOperationType().toString());
            adminEventLog.append(" ");
        }

        if (adminEvent.getAuthDetails() != null) {
            if (adminEvent.getAuthDetails().getRealmId() != null) {
                adminEventLog.append(", realm: ");
                adminEventLog.append(adminEvent.getAuthDetails().getRealmId());
            }

            if (adminEvent.getAuthDetails().getClientId() != null) {
                adminEventLog.append(", clientId: ");
                adminEventLog.append(adminEvent.getAuthDetails().getClientId());
            }

            if (adminEvent.getAuthDetails().getUserId() != null) {
                adminEventLog.append(", userId: ");
                adminEventLog.append(adminEvent.getAuthDetails().getUserId());
            }

            if (adminEvent.getAuthDetails().getIpAddress() != null) {
                adminEventLog.append(", ipAddress: ");
                adminEventLog.append(adminEvent.getAuthDetails().getIpAddress());
            }
        }

        if (adminEvent.getResourceType() != null) {
            adminEventLog.append(", resourceType: ");
            adminEventLog.append(adminEvent.getResourceType().toString());
            adminEventLog.append(" ");
        }

        if (adminEvent.getResourcePath() != null) {
            adminEventLog.append(", resourcePath: ");
            adminEventLog.append(adminEvent.getResourcePath());
            adminEventLog.append(" ");
        }

        if (adminEvent.getError() != null) {
            adminEventLog.append(", error: ");
            adminEventLog.append(adminEvent.getError());
        }

        adminEventLog.append(" ");
        return adminEventLog.toString();
    }

}
