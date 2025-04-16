package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.KeyCloakClientCreationService;
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
import java.util.Optional;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  private final ListApiKeysService listKeysService;

  public ApiKeyValidationProvider(KeycloakSession keycloakSession) {
    this.session =keycloakSession;
    service = new ApiKeyValidationService(session);
    listKeysService = new ListApiKeysService();
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
    if(result!= null && result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    return Response.status(Status.NO_CONTENT).build();
  }


  @Path("/{clientPublicId}")
  @DELETE
  public Response disableApikey(@PathParam("clientPublicId") String clientPublicID){

    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if (!result.isSuccess()) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    return checkAndDisableApiKey(clientPublicID, result.getUser());
  }

  private Response checkAndDisableApiKey(String clientPublicID, UserModel user) {
    //Get Clients Associated to User
    List<Apikey> clientList = listKeysService.getPrivateAndProjectkeys(user);
    ClientModel clientToBeDisabled = session.clients().getClientById(session.getContext().getRealm(),
        clientPublicID);
    if(clientToBeDisabled==null){
      return Response.status(Status.NOT_FOUND).entity(ErrorMessage.CLIENT_UNKNOWN_404).build();
    }
    //check if requested client is part of clients associated to authorized user
    if (clientList.stream().noneMatch(apikey -> apikey.getId().equals(clientToBeDisabled.getId()))) {
      return Response.status(Status.FORBIDDEN).entity(ErrorMessage.USER_NOT_AUTHORIZED_403).build();
    }
    //disable the key
    clientToBeDisabled.setEnabled(false);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Path("")
  @POST
  public Response registerPersonalKey() {
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if (!result.isSuccess()) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    UserModel userModel = result.getUser();
    //check if user already owns any personal key
    if (getPersonalKey(userModel) != null) {
      return Response.status(Status.BAD_REQUEST).entity(ErrorMessage.DUPLICATE_KEY_400).build();
    }
    //Create new key and associate it to user
    KeyCloakClientCreationService service = new KeyCloakClientCreationService(session,
        null, userModel.getUsername());
    Apikey apikey = service.registerPersonalKey(userModel);
    return Response.status(Status.OK).entity(apikey).build();
  }

  private static ClientModel getPersonalKey(UserModel userModel) {
    Optional<RoleModel> clientOwnerRole = userModel.getRoleMappingsStream().filter(
        roleModel -> (Constants.CLIENT_OWNER.equals(roleModel.getName()))).findFirst();
     return (clientOwnerRole.map(roleModel -> (ClientModel) roleModel.getContainer()).orElse(null));
  }
}
