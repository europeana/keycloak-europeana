package eu.europeana.keycloak.zoho.timer;

import eu.europeana.keycloak.timer.AbstractCustomScheduledTask;
import eu.europeana.keycloak.zoho.ZohoSyncService;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class ZohoSyncTask extends AbstractCustomScheduledTask {
    private static final Logger LOG = Logger.getLogger(ZohoSyncTask.class);
    @Override
    public String getTaskName() {
        return "zohosynctask";
    }

    @Override
    public void process(KeycloakSession session) {
        runSyncService(session);
    }

    private void runSyncService(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        KeycloakModelUtils.runJobInTransaction(sessionFactory, backgroundSession -> {
            try {
                //copy realm to backGroundSession
                RealmModel realm = backgroundSession.realms().getRealm("europeana");
                backgroundSession.getContext().setRealm(realm);
                ZohoSyncService service = new ZohoSyncService(backgroundSession);
                service.runZohoSync(1);

            } catch (Throwable t) {
                //Throwable is used instead to capture Both Exceptions and Errors from the job
                LOG.error("Error while running zoho sync - " + t);
                throw t;
            }
        });

    }
}