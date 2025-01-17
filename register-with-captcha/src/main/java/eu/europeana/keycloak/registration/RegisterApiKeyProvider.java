package eu.europeana.keycloak.registration;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class RegisterApiKeyProvider implements RealmResourceProvider {
  private final KeycloakSession session;
  public RegisterApiKeyProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public Object getResource() {
    return new RegistrationService(this.session);
  }

  @Override
  public void close() {
   //No custom implementation required
  }
}
