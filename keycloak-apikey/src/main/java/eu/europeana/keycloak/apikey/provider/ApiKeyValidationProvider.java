package eu.europeana.keycloak.apikey.provider;

import eu.europeana.keycloak.apikey.service.ApiKeyValidationService;
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
