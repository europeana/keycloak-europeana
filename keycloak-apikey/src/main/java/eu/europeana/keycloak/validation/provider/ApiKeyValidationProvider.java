package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationProvider.class);

  public ApiKeyValidationProvider(KeycloakSession keycloakSession) {
    this.session =keycloakSession;
    service = new ApiKeyValidationService(session);
  }

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {
    //specific implementation not required . e.g resource or connection cleanup
  }

  @Path("/validate")
  @POST
  public Response validateApiKey(@QueryParam("client_id") String clientId , @QueryParam("ip") String ip ) {
    //validate token,apikey then IP in that order
    ValidationResult result = service.validateAuthToken();
    if (result.getErrorResponse() == null) {
      result = service.validateApikey(clientId);
    }
    if(result.getErrorResponse() == null){
      result = service.validateIp(ip);
    }
    //TODO - Update Logic to consume the validated IP
    if(result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    return Response.status(Status.NO_CONTENT).build();
  }


  @Path("/{client_id}")
  @POST
  public Response disableApikey(@PathParam("client_id") String clientId){
    //validate token and get user
    //check if key owned by user if not return 403
    //disable the key
    ValidationResult result = service.validateAuthToken();
    if (result.getErrorResponse() == null) {
      UserModel userModel = result.getUser();
      if(userModel == null){
        return Response.status(Status.NOT_FOUND).build();
      }

      ErrorMessage errorResponse = service.validateApikey(clientId).getErrorResponse();
      if(errorResponse !=null){
        return Response.status(result.getHttpStatus()).entity(errorResponse).build();
      }

      //check if key owned by authenticated user
      List<Apikey> clientList = service.getPrivateAndProjectkeys(userModel);
      ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), clientId);
      if(client!= null && clientList.stream().anyMatch(apikey -> apikey.getId().equals(client.getId()))){
       //disable the key
        client.setEnabled(false);
        LOG.info("Key "+clientId+" is disabled.");
      }
      else{
        LOG.error("Error disabling the key "+clientId);
      }
    }
    return Response.status(Status.NO_CONTENT).build();
  }

}
