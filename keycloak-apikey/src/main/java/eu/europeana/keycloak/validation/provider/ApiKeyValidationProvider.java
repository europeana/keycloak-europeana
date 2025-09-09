package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.KeyCloakClientCreationService;
import eu.europeana.keycloak.validation.service.ListApiKeysService;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.Cors;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  public static final int ALLOWED_NUMBER_OF_DISABLED_KEYS = 3;
  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  private final ListApiKeysService listKeysService;
  private Cors cors;

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
    //validate token,apikey(i.e. clientId),IP and then rateLimit in that order
    ValidationResult result = service.validateAuthToken(null);
    ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), clientId);

    if (result.getErrorResponse() == null) {
      result = service.validateClient(client);
    }
    if (result.getErrorResponse() == null){
      result = service.validateIp(ip);
    }
    if (result.getErrorResponse() == null){
      result = service.performRateLimitCheck(client);
    }
    //TODO - Update Logic to consume the validated IP
    if (result!= null && result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    return Response.status(Status.NO_CONTENT).build();
  }


  @Path("/{clientPublicId}")
  @DELETE
  public Response disableApikey(@PathParam("clientPublicId") String clientPublicID){
    setupCors("DELETE");
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if (!result.isSuccess()) {
      return this.cors.builder(Response.status(result.getHttpStatus()).entity(result.getErrorResponse())).build();
    }
    return checkAndDisableApiKey(clientPublicID, result.getUser());
  }

  @Path("/{clientPublicId}")
  @OPTIONS
  public Response disableApikeyPreflight() {
    return Cors.add(session.getContext().getHttpRequest(), Response.ok())
        .auth().allowedMethods("DELETE")
        .preflight().build();
  }

  private Response checkAndDisableApiKey(String clientPublicID, UserModel user) {
    //Get Clients Associated to User
    ClientModel clientToBeDisabled = session.clients().getClientById(session.getContext().getRealm(),
        clientPublicID);
    if (clientToBeDisabled==null){
      return this.cors.builder(Response.status(Status.NOT_FOUND).entity(ErrorMessage.CLIENT_UNKNOWN_404)).build();
    }
    List<Apikey> clientList = listKeysService.getPrivateAndProjectkeys(user);
    //check if requested client is part of clients associated to authorized user
    if (clientList.stream().noneMatch(apikey -> apikey.getId().equals(clientToBeDisabled.getId()))) {
      return this.cors.builder(Response.status(Status.FORBIDDEN).entity(ErrorMessage.USER_NOT_AUTHORIZED_403)).build();
    }
    //check if key already disabled
    if (!clientToBeDisabled.isEnabled()){
      return this.cors.builder(Response.status(Status.GONE).entity(ErrorMessage.CLIENT_ALREADY_DISABLED_410)).build();
    }
    //disable the key
    clientToBeDisabled.setEnabled(false);
    return this.cors.builder(Response.status(Status.NO_CONTENT)).build();
  }

  @Path("")
  @POST
  public Response registerPersonalKey() {
    setupCors("POST");
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if (!result.isSuccess()) {
      return this.cors.builder(Response.status(result.getHttpStatus()).entity(result.getErrorResponse())).build();
    }
    UserModel userModel = result.getUser();
    List<ClientModel> personalKeys = getPersonalKeys(userModel);
    if (!personalKeys.isEmpty()) {
      if (personalKeys.stream().anyMatch(ClientModel::isEnabled)){
        return this.cors.builder(Response.status(Status.BAD_REQUEST).entity(ErrorMessage.DUPLICATE_KEY_400)).build();
      }
      //check if user owns 3 disabled personal keys
      if (personalKeys.stream().filter(p-> !p.isEnabled()).count() == ALLOWED_NUMBER_OF_DISABLED_KEYS){
        return this.cors.builder(Response.status(Status.BAD_REQUEST).entity(ErrorMessage.KEY_LIMIT_REACHED_400)).build();
      }
    }
    //Create new key and associate it to user
    KeyCloakClientCreationService clientCreationService = new KeyCloakClientCreationService(session,
        null, userModel.getUsername());
    Apikey apikey = clientCreationService.registerKey(userModel,Constants.PERSONAL_KEY,Constants.PRIVATE_KEY_DESCRIPTION);
    return this.cors.builder(Response.status(Status.OK).entity(apikey)).build();
  }

  @Path("")
  @OPTIONS
  public Response registerPersonalKeyPreflight() {
    return Cors.add(session.getContext().getHttpRequest(), Response.ok())
          .auth().allowedMethods("POST")
          .preflight().build();
  }



  private static List<ClientModel> getPersonalKeys(UserModel userModel) {
    List<RoleModel> clientOwnerRoles = userModel.getRoleMappingsStream().filter(
            roleModel -> (Constants.CLIENT_OWNER.equals(roleModel.getName()))).toList();
    List<ClientModel> clientList = new ArrayList<>();
    clientOwnerRoles.forEach(p-> clientList.add((ClientModel) p.getContainer()));
   return clientList;

  }

  private void setupCors(String allowedMethod) {
    this.cors = Cors.add(session.getContext().getHttpRequest())
        .auth().allowedMethods(allowedMethod)
        .exposedHeaders("Allow")
        .allowAllOrigins();
  }


}