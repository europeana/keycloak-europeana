package eu.europeana.keycloak.timer;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;


/**
 * Base abstract class for scheduled tasks. Ensures the
 * scheduled task is executed only ones on node in cluster at a given time.
 */

public abstract class AbstractCustomScheduledTask implements ScheduledTask {

  public static final String WORK_CACHE = "work";
  public static final long LOCK_DURATION = 60000L;
  private static final Logger LOG = Logger.getLogger(AbstractCustomScheduledTask.class);
  public abstract String getTaskName();

  /**
   * Run the specific logic by first acquiring lock.If unable to acquire the lock , The process execution is skipped.
   * @param session Keycloak session
   */
  @Override
  public void run(KeycloakSession session) {

    LOG.info("Acquiring the lock to execute the task "+getTaskName());
    if (acquireLock(session)) {
      //Execute task
      process(session);
    } else {
      LOG.info("Unable to acquire lock for clearing cache , skipping the execution !!");
    }
  }

  /**
   * Logic to perform actual task , method to be called ofter acquiring the lock.
   * @param session Keycloak session
   */
  public abstract void process(KeycloakSession session) ;

  /**
   * Creates the entry for specific task-lock in keycloaks distributed 'work' cache with fixed expiry time.
   * If the lock is expired its replaced with new entry.
   * @param session Keycloak session
   * @return boolean 'true' indicating lock is acquired successfully
   */
  public boolean acquireLock(KeycloakSession session) {
    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
    Cache<String,Long> workCache = provider.getCache(WORK_CACHE);
    if(workCache != null) {
      long now = System.currentTimeMillis();
      long timeWhenLockExpires = now + LOCK_DURATION;

      Long previousExpiry
          = workCache.getAdvancedCache()
          .withFlags(Flag.FORCE_SYNCHRONOUS)
          .putIfAbsent(getTaskName(), timeWhenLockExpires);

      if (previousExpiry == null) {
        LOG.info("Acquired the Lock with expiry on "+ timeWhenLockExpires);
        return true;
      } else if  ((previousExpiry < now) &&
         (workCache.getAdvancedCache()
            .withFlags(Flag.FORCE_SYNCHRONOUS)
            .replace(getTaskName(), previousExpiry, timeWhenLockExpires)) ) {
          LOG.info("Took over the expired Lock and new expiry is "+ timeWhenLockExpires);
          return true;
        }
    }
    LOG.error("workCache not present !!");
    return false;
  }
}