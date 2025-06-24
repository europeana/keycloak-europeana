package eu.europeana.keycloak.validation.provider;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory class for getting  ApiKeyLegacyValidationProvider instance
 */
public class ApiKeyLegacyValidationProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="validate_legacy";
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new ApiKeyLegacyValidationProvider(keycloakSession);
  }

  @Override
  public void init(Scope scope) {
    //specific init actions not required e.g. reading,parsing configuration parameters,establishing initial connections to external resources
  }
  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    //specific postInit actions not required e.g. establish connections to resources that other providers have instantiated
  }

  @Override
  public void close() {
    //specific implementation not required . e.g resource or connection cleanup
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}