package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.ListApiKeysService;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserApiKeysProvider implements RealmResourceProvider {

  private final ApiKeyValidationService validationService;
  private final ListApiKeysService listKeysService;

  public UserApiKeysProvider(KeycloakSession keycloakSession) {
    this.validationService = new ApiKeyValidationService(keycloakSession);
    this.listKeysService = new ListApiKeysService();
  }
  @Override
  public Object getResource() {
    return this;
  }
  @Override
  public void close() {
    //specific implementation not required . e.g resource or connection cleanup
  }

  @Path("/clients")
  @GET
  @Produces("application/json")
  public Response getKeysAssociatedToUser(){
    ValidationResult result = validationService.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if(result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    return Response.ok().entity(listKeysService.getPrivateAndProjectkeys(result.getUser())).build();
  }

}
