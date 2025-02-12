package eu.europeana.keycloak.validation.provider;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ApiKeyLegacyValidationProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="validate_legacy";
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new ApiKeyLegacyValidationProvider(keycloakSession);
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
