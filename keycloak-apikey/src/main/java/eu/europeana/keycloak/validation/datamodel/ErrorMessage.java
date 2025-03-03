package eu.europeana.keycloak.validation.datamodel;

public enum ErrorMessage {
  TOKEN_INVALID_401("401_token_invalid","Token is invalid","Please acquire a new token or get in contact with the Europeana APIs customer support via api@europeana.eu"),
  TOKEN_MISSING_401("401_token_missing","Token is missing","Please issue a token and supply it within the Authorization header."),
  TOKEN_EXPIRED_401("401_token_expired","Token has expired","Please acquire a new token by either log-in or refreshing using the refresh token"),
  KEY_INVALID_401("401_key_invalid", "API key is invalid", "Please register for an API key"),
  KEY_DISABLED_401("401_key_disabled", "You API key has been disabled", "Please register for a new API key"),
  IP_MISSING_400("400_ip_missing","",""),
  IP_INVALID_400("400_ip_invalid","",""),

  SCOPE_MISSING_403("403_scope_missing",
      "Client not authorised due to missing scope access",
      "The client does not have access to this service. Please get in contact with the Europeana APIs customer support via api@europeana.eu");
  public final String code;
  public final String error;
  public final String message;

  ErrorMessage(String code, String error, String message) {
    this.code = code;
    this.error = error;
    this.message = message;
  } 
}
