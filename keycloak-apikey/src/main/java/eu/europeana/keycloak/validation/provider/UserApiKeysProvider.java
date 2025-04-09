package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserApiKeysProvider implements RealmResourceProvider {

  private final ApiKeyValidationService service;

  public UserApiKeysProvider(KeycloakSession keycloakSession) {

    this.service = new ApiKeyValidationService(keycloakSession);
  }
  @Override
  public Object getResource() {
    return this;
  }
  @Override
  public void close() {
  }

  @Path("/clients")
  @GET
  @Produces("application/json")
  public Response getKeysAssociatedToUser(){
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if(result.getErrorResponse() != null) {
      return Response.status(result.getHttpStatus()).entity(result.getErrorResponse()).build();
    }
    List<Apikey> apikeyList =   service.getPrivateAndProjectkeys(result.getUser());
    return Response.ok().entity(apikeyList).build();
  }

}
