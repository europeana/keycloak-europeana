package eu.europeana.keycloak.zoho;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
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
     * @param days  contacts
     * @return http 202 is the request is accepted , 409
     */
    @Path("")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response zohoSync(@DefaultValue("1") @QueryParam("days") int days) {
        //Get the status currently running sync job from cache
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String,String> workCache = provider.getCache("work");

        //Update job status in replicated cache , to make status accessible on other POS in cluster
        if(workCache.putIfAbsent(SYNC_JOB_STATUS, RUNNING)!=null) {
           return Response.status(Response.Status.CONFLICT)
                   .entity("{\"error\" :\"Sync job already running.\"}")
                   .build();
        }

        //fetch the realm ID of the current session
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        String realmID = session.getContext().getRealm().getId();

        //Run the process in background
        CompletableFuture.runAsync(() -> {
            try{
                //When we return the immediate response the outer session gets closed , creating new session object for background task
                runBackgroundJob(days, sessionFactory, realmID);
            } catch (Throwable t) {
                LOG.error("Background job failed - " + t);
            } finally {
                //remove the status from cache ones task is completed.Need to use separate session for this.
                KeycloakModelUtils.runJobInTransaction(sessionFactory, sessionToUpdateJobStatus -> {
                    sessionToUpdateJobStatus.getProvider(InfinispanConnectionProvider.class)
                            .getCache("work")
                            .remove(SYNC_JOB_STATUS);
                });
            }
        });
        return Response.status(Response.Status.ACCEPTED)
                .entity("\"message\" : \"Sync job started in background.\"")
                .build();
    }

    private static void runBackgroundJob(int days, KeycloakSessionFactory sessionFactory, String realmID) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, backgroundSession -> {
         try {
             //copy realm to backGroundSession
             RealmModel realm = backgroundSession.realms().getRealm(realmID);
             backgroundSession.getContext().setRealm(realm);

             LOG.info("Running background task for zoho sync !! ");
             ZohoSyncService service = new ZohoSyncService(backgroundSession);
             service.runZohoSync(days);
         } catch (Throwable t) {
             LOG.error("Error while running zoho sync - " + t);
             throw t;
         }
     });
    }

    @Path("/job-status/reset")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response overrideSyncJobStatus(){
        String status = (String) session.getProvider(InfinispanConnectionProvider.class)
                .getCache("work")
                .remove(SYNC_JOB_STATUS);
        return Response.ok().entity("\"message\" : \"Job status cleared! Previous value"+status+"\" ").build();
    }

    @Override
    public void close() {
        // No action required
    }

}