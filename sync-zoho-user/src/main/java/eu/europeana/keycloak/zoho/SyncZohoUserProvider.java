package eu.europeana.keycloak.zoho;

import eu.europeana.keycloak.zoho.timer.ZohoSyncTask;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Created by luthien on 14/11/2022.
 */
@SuppressWarnings("javasecurity:S6096")
@Provider
public class SyncZohoUserProvider implements RealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(SyncZohoUserProvider.class);
    public static final String SYNC_JOB_STATUS = "syncJobStatus";
    public static final String RUNNING = "RUNNING";
    KeycloakSession session ;

    public SyncZohoUserProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Runs zoho syn in background
     * @param days contacts modified in zoho till this many days ago are updated in keycloak.
     * @return http 202 is the request is accepted , 409
     */
    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response zohoSync(@DefaultValue("1") @QueryParam("days") int days) {

        //fetch the realm ID of the current session
        RealmModel realm = session.getContext().getRealm();
        String realmID = session.getContext().getRealm().getId();

        //Get the status of currently running sync job from DB
        String status = realm.getAttribute(SYNC_JOB_STATUS);
        if(RUNNING.equals(status)) {
            return Response.status(Response.Status.OK)
                    .entity("{\"error\" :\"Sync job already running.\"}")
                    .build();
        }
        //Update the Job status on realm
        realm.setAttribute(SYNC_JOB_STATUS,RUNNING);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        //Run the process in background
        CompletableFuture.runAsync(() -> {
            try{
                //When we return the immediate response the outer session gets closed , creating new session object for background task
                ZohoSyncTask task = new ZohoSyncTask();
                task.runBackgroundJob(session);
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
        return Response.status(Response.Status.ACCEPTED)
                .entity("\"message\" : \"Sync job started in background.\"")
                .build();
    }

    @Override
    public void close() {
        // No action required
    }

}