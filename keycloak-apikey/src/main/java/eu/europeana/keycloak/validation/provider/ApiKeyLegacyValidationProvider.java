package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.datamodel.ErrorResponseLegacy;
import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyLegacyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  public static final String APIKEY_MISSING = "No API key in header. Correct header syntax 'Authorization: APIKEY <your_key_here>'";
   public static final String APIKEY_NOT_REGISTERED  = "\"API key %s is not registered\"";


  public ApiKeyLegacyValidationProvider(KeycloakSession keycloakSession) {
    this.session =keycloakSession;
    service = new ApiKeyValidationService(session);
  }

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {
  }

  @Path("")
  @POST
  public Response validateApiKeyLegacy(){
    HttpRequest httpRequest = session.getContext().getHttpRequest();
    String apikey = service.extractApikeyFromAuthorizationHeader(httpRequest);
    if (StringUtils.isBlank(apikey)) {
      ErrorResponseLegacy errorResponse = new ErrorResponseLegacy(Status.BAD_REQUEST.getStatusCode(),
          Status.BAD_REQUEST.getReasonPhrase(),
          APIKEY_MISSING,
          httpRequest.getUri().getPath());
      return Response.status(Status.BAD_REQUEST).entity(errorResponse).build();
    }
    if(!service.validateApikeyLegacy(apikey)){
      return Response.status(Status.UNAUTHORIZED).entity(String.format(APIKEY_NOT_REGISTERED, apikey)).build();
    }
    //TODO - Update the session history table
    return Response.status(Status.NO_CONTENT).build();
  }

}
