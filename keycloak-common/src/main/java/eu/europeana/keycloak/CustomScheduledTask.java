package eu.europeana.keycloak;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

public abstract class CustomScheduledTask implements ScheduledTask {

  public static final String WORK_CACHE = "workCache";
  private final String taskName;

  public static final long LOCK_DURATION = 60000L;

  private Logger LOG = Logger.getLogger(CustomScheduledTask.class);

  public String getTaskName() {
    return taskName;
  }

  public CustomScheduledTask(String taskName) {
    this.taskName = taskName;
  }


  @Override
  public void run(KeycloakSession session) {
    if (acquireLock(session)) {
      //Execute task
      process(session);
    } else {
      LOG.info("Unable to acquire lock for clearing cache , skipping the execution !!");
    }
  }

  public abstract void process(KeycloakSession session) ;

  public boolean acquireLock(KeycloakSession session) {
    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
    Cache<String,Long> workCache = provider.getCache(WORK_CACHE);
    if(workCache != null) {
      long now = System.currentTimeMillis();
      long timeWhenLockExpires = now + LOCK_DURATION;

      Long previousExpiry
          = workCache.getAdvancedCache()
          .withFlags(Flag.FORCE_SYNCHRONOUS)
          .putIfAbsent(taskName, timeWhenLockExpires);

      if (previousExpiry == null) {
        LOG.info("Acquired the Lock with expiry on "+timeWhenLockExpires);
        return true;
      } else if (previousExpiry < now) {
        if (workCache.getAdvancedCache()
            .withFlags(Flag.FORCE_SYNCHRONOUS)
            .replace(taskName, previousExpiry, timeWhenLockExpires)) {
          LOG.info("Took over the expired Lock and new expiry is "+timeWhenLockExpires);
          return true;
        }
      }
    }
    LOG.error("workCache not present !!");
    return false;
  }
}