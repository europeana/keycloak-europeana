package eu.europeana.keycloak.registration.service;

import eu.europeana.keycloak.registration.exception.ClientCreationException;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class KeyCloakClientCreationService  {
  private static final Logger LOG = Logger.getLogger(KeyCloakClientCreationService.class);
   private final KeycloakSession session;
   private final RealmModel realm;
   private final String clientID;
  private final String clientName;

   public KeyCloakClientCreationService(RealmModel realm, KeycloakSession session,String clientID,String clientName) {
     this.session = session;
     this.realm =realm;
     this.clientID = clientID;
     this.clientName = clientName;
   }

   public ClientModel registerClient() throws ClientCreationException {
     // The client should have the “Capability config“ section completely disabled so that this client cannot be used to issue any kind of tokens.
     //Set the name of the user as the name of the client
     //Set the description as “Private key for individual use only“
     //Create the role “client_owner“ for the newly created client
     //Associate the role “client_owner“ of the client to the user
     return getClientModel();
   }

  private ClientModel getClientModel() {
    ClientModel client;
     try {
       ClientProvider clientProvider = session.clients();
       client = clientProvider.addClient(realm, clientID);
       client.setName(clientName);
       client.setDescription("Private key for individual use only");
       client.setProtocol("openid-connect");
       client.setPublicClient(true);
       client.setStandardFlowEnabled(false);
       client.setImplicitFlowEnabled(false);
       client.setDirectAccessGrantsEnabled(false);
       client.setServiceAccountsEnabled(false);

       LOG.info("Created new client : "+ client.getName() + " "+client.getClientId());

     }catch (Exception ex){
       LOG.error("Unable to create new Client !! ");
       throw new ClientCreationException(ex.getMessage(),ex);
     }
    return client;
  }
}
