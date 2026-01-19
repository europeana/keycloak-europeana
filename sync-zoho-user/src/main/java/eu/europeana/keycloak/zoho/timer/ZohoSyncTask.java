package eu.europeana.keycloak.zoho.timer;

import eu.europeana.keycloak.timer.AbstractCustomScheduledTask;
import eu.europeana.keycloak.zoho.ZohoSyncService;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.concurrent.CompletableFuture;

public class ZohoSyncTask extends AbstractCustomScheduledTask {

    private static final Logger LOG = Logger.getLogger(ZohoSyncTask.class);

    public static final String SYNC_JOB_STATUS = "syncJobStatus";
    public static final String RUNNING = "RUNNING";
    public static final String EUROPEANA = "europeana";
    @Override
    public String getTaskName() {
        return "zohosynctask";
    }

    @Override
    public void execute(KeycloakSession session) {
        runSyncService(session);
    }

    private void runSyncService(KeycloakSession session) {

        //fetch the realm ID of the current session
        RealmModel realm = session.getContext().getRealm();
        String realmID = session.getContext().getRealm().getId();

        //Get the status of currently running sync job from DB
        String status = realm.getAttribute(SYNC_JOB_STATUS);


        if(RUNNING.equals(status)) {
            LOG.info("Sync job already running.");
            return ;
        }
        //Update the Job status on realm
        realm.setAttribute(SYNC_JOB_STATUS,RUNNING);

        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        //Run the process in background
        CompletableFuture.runAsync(() -> {
            try{
                runBackgroundJob(session);
            } catch (Throwable t) {
                LOG.error("Background job failed - " + t);
            } finally {
                //remove the status from cache ones task is completed.Need to use separate session for this.
                KeycloakModelUtils.runJobInTransaction(sessionFactory, sessionToUpdateJobStatus -> {
                    RealmModel realmForStatusUpdate = sessionToUpdateJobStatus.realms().getRealm(realmID);
                    realmForStatusUpdate.removeAttribute(SYNC_JOB_STATUS);
                });
            }
        });
        LOG.info("Sync job Started in background !!");

    }

    public void runBackgroundJob(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        KeycloakModelUtils.runJobInTransaction(sessionFactory, backgroundSession -> {
            try {
                //copy realm to backGroundSession,
                // The 'Users' or 'Clients' are always added in 'euroepena' realm
                RealmModel realm = backgroundSession.realms().getRealm(EUROPEANA);
                backgroundSession.getContext().setRealm(realm);
                ZohoSyncService service = new ZohoSyncService(backgroundSession);
                //sync service checks which contacts were modified in zoho in last 'n' days
                // and then copies relevant data of those
                //contacts to keycloak. By default, it checks for contacts changed in last 1 day.
                service.runZohoSync(1);

            } catch (Throwable t) {
                //Throwable is used instead to capture Both Exceptions and Errors from the job
                LOG.error("Error while running zoho sync - " + t);
                throw t;
            }
        });
    }
}