package eu.europeana.keycloak.sessions.provider;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ApiKeyValidationProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="validate";
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new ApiKeyValidationProvider(keycloakSession);
  }

  @Override
  public void init(Scope scope) {
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
}
