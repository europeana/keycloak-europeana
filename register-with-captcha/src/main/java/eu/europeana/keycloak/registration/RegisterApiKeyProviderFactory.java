package eu.europeana.keycloak.registration;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class RegisterApiKeyProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="register-with-captcha";
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new RegisterApiKeyProvider(keycloakSession);
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public void init(org.keycloak.Config.Scope scope) {

  }
}
