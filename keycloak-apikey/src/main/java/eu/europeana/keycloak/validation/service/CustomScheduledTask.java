package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import eu.europeana.keycloak.validation.util.Constants;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

public class CustomScheduledTask implements ScheduledTask {

  private final Logger LOG = Logger.getLogger(CustomScheduledTask.class);


  @Override
  public void run(KeycloakSession session) {
     //Clear the session cache
    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
    Cache<String, SessionTracker> sessionTrackerCache = provider.getCache("sessionTrackerCache");
    if (!sessionTrackerCache.isEmpty()){
      updateLastAccessDate(session,sessionTrackerCache);
      sessionTrackerCache.clear();
      LOG.info("Infinispan cache 'sessionTrackerCache' is cleared");
    }
    LOG.info("Infinispan cache 'sessionTrackerCache' is already empty");
  }

  private void updateLastAccessDate(KeycloakSession session,Cache<String, SessionTracker> sessionTrackerCache) {
    for (Map.Entry<String, SessionTracker> entry : sessionTrackerCache.entrySet()) {
      ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), entry.getKey());
      SessionTracker tracker = entry.getValue();
      if(client!=null && tracker !=null && tracker.getLastAccessDateString()!=null) {
        updateLastAccessAttribute(client, tracker.getLastAccessDateString());
      }
    }
  }

  private static void updateLastAccessAttribute(ClientModel client, String lastAccessDate) {
    Stream.of(Constants.CLIENT_OWNER,Constants.SHARED_OWNER)
        .map(client::getRole)
        .filter(Objects::nonNull)
        .forEach(roleModel -> roleModel.setAttribute(Constants.ROLE_ATTRIBUTE_LAST_ACCESS_DATE,
            List.of(lastAccessDate)));
  }

}