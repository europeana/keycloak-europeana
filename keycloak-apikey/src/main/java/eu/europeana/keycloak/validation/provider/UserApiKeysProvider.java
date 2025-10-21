package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.ListApiKeysService;
import eu.europeana.keycloak.utils.Constants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.Cors;

/**
 * Provider class for fetching keys(clients) associated to keycloak user
 */
public class UserApiKeysProvider implements RealmResourceProvider {
  private final ApiKeyValidationService validationService;
  private final ListApiKeysService listKeysService;
  private final KeycloakSession session;
  private Cors cors;

  /**
   * Initialize {@code UserApiKeysProvider} with details
   * @param keycloakSession current Keycloak session
   */
  public UserApiKeysProvider(KeycloakSession keycloakSession) {
    this.session = keycloakSession;
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
    setupCors();
    ValidationResult result = validationService.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if(result.getErrorResponse() != null) {
      return this.cors.builder(Response.status(result.getHttpStatus()).entity(result.getErrorResponse())).build();
    }
    return this.cors.builder(Response.ok().entity(listKeysService.getPrivateAndProjectkeys(result.getUser()))).build();
  }

  @Path("/clients")
  @OPTIONS
  public Response getKeysAssociatedToUserPreflight(){
    return Cors.add(session.getContext().getHttpRequest(), Response.ok())
        .auth().allowedMethods("GET")
        .preflight().build();
  }

  private void setupCors() {
    this.cors = Cors.add(session.getContext().getHttpRequest())
        .auth().allowedMethods("GET")
        .exposedHeaders("Allow")
        .allowAllOrigins();
  }
}