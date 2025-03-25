package eu.europeana.keycloak.registration.provider;

import org.keycloak.Config.Scope;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomEmailSenderProviderFactory implements EmailSenderProviderFactory {

  public static final String CUSTOM_EMAIL_PROVIDER = "custom-email-provider";

  @Override
  public EmailSenderProvider create(KeycloakSession keycloakSession) {
    return new CustomEmailSenderProvider(keycloakSession);
  }

  @Override
  public void init(Scope scope) {
    // not required
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    // not required
  }

  @Override
  public void close() {
    // not required
  }

  @Override
  public String getId() {
    return CUSTOM_EMAIL_PROVIDER;
  }
}
