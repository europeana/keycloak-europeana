package eu.europeana.keycloak.logging;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

public class EuropeanaEventListenerProvider implements EventListenerProvider {

    private final String prefix;
    private final  Logger log;

    KeycloakSession session;

    public EuropeanaEventListenerProvider(Logger log, String prefix,KeycloakSession session) {
        this.log = log;
        this.prefix = prefix;
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        String msg = prefix + formatEventLog(event);
        log.info(msg);
        if(EventType.CODE_TO_TOKEN.equals(event.getType())){
            String clientID = event.getClientId();
            ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), clientID);
            RoleModel clientOwnerRole = client.getRole("client_owner");
            RoleModel shareOwnerRole = client.getRole("shared_owner");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(
                ZoneOffset.UTC);
            Date lastAccessTime= new Date();
            if (clientOwnerRole != null) {
                clientOwnerRole.setAttribute("lastAccessDate",
                    List.of(formatter.format(lastAccessTime.toInstant())));
            }
            if (shareOwnerRole != null) {
                shareOwnerRole.setAttribute("lastAccessDate",
                    List.of(formatter.format(lastAccessTime.toInstant())));
            }
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        String msg = prefix + formatEventLog(adminEvent);
        log.info(msg);
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