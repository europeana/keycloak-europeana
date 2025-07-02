package eu.europeana.keycloak.validation.provider;

import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Provider class for additional admin resources
 */
public class CustomAdminResourceProvider implements RealmResourceProvider {

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {
   //No specific Implementation required
  }
}