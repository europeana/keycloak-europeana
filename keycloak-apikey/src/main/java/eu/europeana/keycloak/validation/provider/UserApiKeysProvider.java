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
import org.keycloak.services.cors.Cors;

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
      return this.cors.add(Response.status(result.getHttpStatus()).entity(result.getErrorResponse()));
    }
    return this.cors.add(Response.ok().entity(listKeysService.getPrivateAndProjectkeys(result.getUser())));
  }

  @Path("/clients")
  @OPTIONS
  public Response getKeysAssociatedToUserPreflight() {
    return session.getProvider(Cors.class)
            .auth()
            .allowedMethods("GET")
            .preflight()
            .add(Response.ok());
  }

  private void setupCors() {
    this.cors = session.getProvider(Cors.class)
            .auth()
            .allowedMethods("GET")
            .exposedHeaders("Allow")
            .allowAllOrigins();
  }
}