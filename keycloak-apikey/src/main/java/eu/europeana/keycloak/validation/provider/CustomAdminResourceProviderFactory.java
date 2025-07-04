package eu.europeana.keycloak.validation.provider;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for creating instances of {@link CustomAdminResourceProvider}.
 * The provider ID is defined as "admin", indicating that this factory
 * will manage resources accessible via a path like "/realms/{realm}/admin/..."
 * It implements the {@link RealmResourceProviderFactory} interface, for extending realm-level REST APIs.
 */

public class CustomAdminResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="admin";
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new CustomAdminResourceProvider(keycloakSession);
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