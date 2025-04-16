package eu.europeana.keycloak.validation.util;

public class Constants {
  private Constants() {
  }
  public static final String CLIENT_SCOPE_APIKEYS = "apikeys";
  public static final String ROLE_ATTRIBUTE_CREATION_DATE = "created";
  public static final String CREATION_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String CLIENT_STATE_DISABLED = "disabled";
  public static final String PERSONAL_KEY = "PersonalKey";
  public static final String PROJECT_KEY = "ProjectKey";
  public static final String GRANT_TYPE_PASSWORD = "password";
  public static final String GRANT_TYPE_CLIENT_CRED = "client_credentials";
  public static final String APIKEY_NOT_REGISTERED = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE = "API key %s is not active";
  public static final String APIKEY_PATTERN = "APIKEY\\s+([^\\s]+)";

  public static final String CLIENT_OWNER = "client_owner";
  public static final String SHARED_OWNER ="shared_owner";
  public static final String CLIENT_OWNER_ROLE_DESCRIPTION="Ownership of this client";
}
