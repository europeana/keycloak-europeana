package eu.europeana.keycloak.sessions.provider;

import eu.europeana.keycloak.sessions.service.ApiKeyValidationService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ApiKeyValidationProvider implements RealmResourceProvider {

  private final KeycloakSession session;
  public ApiKeyValidationProvider(KeycloakSession keycloakSession) {
    this.session =keycloakSession;
  }

  @Override
  public Object getResource() {
    return new ApiKeyValidationService(this.session);
  }

  @Override
  public void close() {

  }
}
