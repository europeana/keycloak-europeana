package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.timer.CustomScheduledTask;
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
import org.keycloak.models.RealmModel;

/**
 *  Performs operations based on session tracking cache and then clears it.
 */
public class ClearSessionTrackingCacheTask extends CustomScheduledTask {

  private static final Logger LOG = Logger.getLogger(ClearSessionTrackingCacheTask.class);

  /**
   * Instantiate the ClearSessionTrackingCacheTask using specific name.
   */
  public ClearSessionTrackingCacheTask() {
    super("clearSessionTrackingCache");
  }
  @Override
  public void process(KeycloakSession session) {
    clearSessionTrackingCache(session);
  }

  private void clearSessionTrackingCache(KeycloakSession session) {
    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
    Cache<String, SessionTracker> sessionTrackerCache = provider.getCache(Constants.SESSION_TRACKER_CACHE);
    if (!sessionTrackerCache.isEmpty()){
      updateClient(session,sessionTrackerCache);
      sessionTrackerCache.clear();
      LOG.info("Infinispan cache 'sessionTrackerCache' is cleared");
    }
    LOG.info("Infinispan cache 'sessionTrackerCache' is already empty");
  }

  private void updateClient(KeycloakSession session,Cache<String, SessionTracker> sessionTrackerCache) {
    for (Map.Entry<String, SessionTracker> entry : sessionTrackerCache.entrySet()) {
      RealmModel europeanaRealm = session.realms().getRealm("europeana");
      if(europeanaRealm == null){
       LOG.error("LastAccessDate not updated in cache. Realm not found - 'europeana'");
       return;
      }

      ClientModel client = session.clients().getClientByClientId(europeanaRealm, entry.getKey());
      SessionTracker tracker = entry.getValue();
      if(client!=null && tracker !=null) {
        if (tracker.getLastAccessDateString() != null) {
          updateClientRoleAtrribute(client, tracker.getLastAccessDateString(),Constants.ROLE_ATTRIBUTE_LAST_ACCESS_DATE);
        }
        if (tracker.getLastRateLimitReachingTime() != null) {
          updateClientRoleAtrribute(client, tracker.getLastRateLimitReachingTimeString(),Constants.ROLE_ATTRIBUTE_LAST_RATELIMIT_REACHING_DATE);
        }
      }
    }
  }

  private static void updateClientRoleAtrribute(ClientModel client, String valueToUpdate,String attributeToUpdate) {
    Stream.of(Constants.CLIENT_OWNER,Constants.SHARED_OWNER)
        .map(client::getRole)
        .filter(Objects::nonNull)
        .forEach(roleModel -> roleModel.setAttribute(attributeToUpdate,
            List.of(valueToUpdate)));
  }

}