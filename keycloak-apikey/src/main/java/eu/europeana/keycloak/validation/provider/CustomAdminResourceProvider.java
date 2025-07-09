package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import eu.europeana.keycloak.validation.service.KeyCloakClientCreationService;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.Cors;

/**
 * Provider class for additional admin resources
 */
public class CustomAdminResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  private Cors cors;
  public CustomAdminResourceProvider(KeycloakSession keycloakSession) {
    this.session = keycloakSession;
    service = new ApiKeyValidationService(session);
  }

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {
   //No specific Implementation required
  }
  @Path("/client")
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response createProjectKey(){
    this.setupCors("POST");
    ValidationResult result = service.validateAuthToken(Constants.GRANT_TYPE_PASSWORD);
    if (!result.isSuccess()) {
      return this.cors.builder(Response.status(result.getHttpStatus()).entity(result.getErrorResponse())).build();
    }
    //allow project key creation only to the authenticated users who have admin role
    UserModel userModel = result.getUser();
    if(userModel.getRoleMappingsStream().noneMatch(p->Constants.ADMIN_ROLE_NAME.equals(p.getName()))){
      return this.cors.builder(Response.status(Status.FORBIDDEN).entity(ErrorMessage.USER_NOT_AUTHORIZED_403)).build();
    }

    //Validate request form data
    MultivaluedMap<String, String> decodedFormParameters = session.getContext().getHttpRequest()
        .getDecodedFormParameters();
    UserModel userForKeyCreation = session.users().getUserByEmail(session.getContext().getRealm(),decodedFormParameters.getFirst("email"));
    //Return error if the type is not for project key or no exists for the specified email
    if(!Constants.PROJECT_KEY.equals(decodedFormParameters.getFirst("type")) || userForKeyCreation ==null){
      return this.cors.builder(Response.status(Status.BAD_REQUEST)).build();
    }

    //Create new key and associate it to user
    KeyCloakClientCreationService clientCreationService = new KeyCloakClientCreationService(session,
        null, userForKeyCreation.getUsername());
    Apikey apikey = clientCreationService.registerKey(userModel,Constants.PROJECT_KEY,decodedFormParameters.getFirst("name"));
    return this.cors.builder(Response.status(Status.OK).entity(apikey)).build();
  }
  private void setupCors(String allowedMethod) {
    this.cors = Cors.add(session.getContext().getHttpRequest())
        .auth().allowedMethods(allowedMethod)
        .exposedHeaders("Allow")
        .allowAllOrigins();
  }

  @Path("/client")
  @OPTIONS
  public Response createProjectKeyPreflight(){
    return Cors.add(session.getContext().getHttpRequest(), Response.ok())
        .auth().allowedMethods("POST")
        .preflight().build();
  }

}