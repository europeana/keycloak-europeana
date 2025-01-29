package eu.europeana.keycloak.sessions.service;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

public class ApiKeyValidationService {
  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationService.class);

  private final KeycloakSession session;

  private final RealmModel realm;

  public static final String APIKEY_NOT_REGISTERED                 = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE                 = "API key %s is not active";

  public ApiKeyValidationService(KeycloakSession session) {
    this.session = session;
    this.realm =  session.getContext().getRealm();
  }


  @Path("/validate")
  @POST
  public Response validateApiKey(@QueryParam("client_id") String clientId , @QueryParam("ip") String ip ){
      BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
      AuthResult authResult = authenticator.authenticate();
      if (authResult == null || !validateApikey(clientId) ) { // ip validation required ?
        return Response.status(Status.BAD_REQUEST).build();  // should be UNAUTHORIZED ?
      }
      //TODO - Update the session history table
      return Response.status(Status.NO_CONTENT).build();
  }



  public boolean validateApikey(String apikey)
  {
    //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientProvider clientProvider = session.clients();
    ClientModel client = clientProvider.getClientByClientId(realm, apikey);
    if(client ==null ){
      LOG.error(String.format(APIKEY_NOT_REGISTERED, apikey));
      return false;
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      LOG.error(String.format(APIKEY_NOT_ACTIVE, client.getClientId()));
      return false;
     }
    return true;
  }

}
