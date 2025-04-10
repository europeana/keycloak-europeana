package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.ListApiKeysService;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  private final ListApiKeysService listKeysService;

  public ApiKeyValidationProvider(KeycloakSession keycloakSession) {
    this.session =keycloakSession;
    service = new ApiKeyValidationService(session);
    listKeysService = new ListApiKeysService(session);
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
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_CLIENT_CRED);
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


  @Path("/{client_public_id}")
  @DELETE
  public Response disableApikey(@PathParam("client_public_id") String client_public_Id){

    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    UserModel user = result.getUser();
    if (result.getErrorResponse()==null) {
      result = service.validateClientById(client_public_Id);
    }
    if (result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    if(!disableKey(client_public_Id,user)){
      return Response.status(Status.FORBIDDEN).build();
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  private boolean disableKey(String clientPublicId, UserModel userModel) {
    List<Apikey> clientList = listKeysService.getPrivateAndProjectkeys(userModel);
    ClientModel client = session.clients().getClientById(session.getContext().getRealm(),
        clientPublicId);
    if(client!= null && clientList.stream().anyMatch(apikey -> apikey.getId().equals(client.getId()))){
      //disable the key
      client.setEnabled(false);
      return true;
    }
    return false;
  }

}
