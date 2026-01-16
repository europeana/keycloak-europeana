package eu.europeana.keycloak.zoho;

import eu.europeana.keycloak.timer.FixedRateTaskScheduler;
import eu.europeana.keycloak.zoho.timer.ZohoSyncTask;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;


/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "zohosync";
    private static final Logger LOG = Logger.getLogger(SyncZohoUserProviderFactory.class);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new SyncZohoUserProvider(session);
    }

    @Override
    public void init(Scope config) {
        //No specific implementation required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //job to be run every day at configured time
        int intervalMinutes = 24*60;
        //Schedule the zoho sync task based on the configured time
        FixedRateTaskScheduler scheduler = new FixedRateTaskScheduler(new ZohoSyncTask(),calculateInitialDelayInMillis(intervalMinutes),intervalMinutes);
        scheduler.scheduleTask(factory);
    }

    private long calculateInitialDelayInMillis(int intervalMinutes) {
        //fetch the configured time for running the job
        LocalTime time = LocalTime.of(2, 0);

        LocalDateTime now = getLocalTime();
        LocalDateTime nextScheduleTime = now.with(time);
        if(now.isAfter(nextScheduleTime)){
            nextScheduleTime = nextScheduleTime.plusDays(1);
        }

        long initialDelay = Duration.between(now,nextScheduleTime).toMillis();
        LOG.info((String.format("Current Time : %s Interval : %s Minutes. Initial delay : %s Millis. Scheduled execution time: %s"
                , now,intervalMinutes,initialDelay,nextScheduleTime)));
        return initialDelay;

    }
    /** Separate method retrieving current dateTime
     *  to allow for unit tests to mock the different times.
     * @return current time
     */
    public LocalDateTime getLocalTime() {
        return LocalDateTime.now();
    }

    @Override
    public void close() {
        //No specific implementation required
    }

}