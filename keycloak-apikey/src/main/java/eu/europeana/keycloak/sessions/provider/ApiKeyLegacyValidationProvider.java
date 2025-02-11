package eu.europeana.keycloak.sessions.provider;

import eu.europeana.keycloak.sessions.service.ApiKeyValidationService;
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

  public static final String APIKEY_MISSING = "Correct header syntax 'Authorization: APIKEY <your_key_here>'";


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
      return Response.status(Status.BAD_REQUEST).entity(APIKEY_MISSING).build();
    }
    //TODO - Update the session history table
    return Response.status(Status.NO_CONTENT).build();
  }

}
