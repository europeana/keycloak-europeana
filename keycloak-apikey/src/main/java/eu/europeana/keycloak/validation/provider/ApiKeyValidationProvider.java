package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.service.ApiKeyValidationService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  private final ApiKeyValidationService service;

  public ApiKeyValidationProvider(KeycloakSession keycloakSession) {
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
  public Response validateApiKey(@QueryParam("client_id") String clientId , @QueryParam("ip") String ip ){
    if (!service.isAuthorized() || !service.validateApikey(clientId) ) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    //TODO - Update the session history table
    return Response.status(Status.NO_CONTENT).build();
  }
}
