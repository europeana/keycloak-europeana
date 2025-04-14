package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"code", "error", "message"})
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorMessage {

  IP_MISSING_400("400_ip_missing","",""),
  IP_INVALID_400("400_ip_invalid","",""),
  TOKEN_INVALID_401("401_token_invalid","Token is invalid","Please acquire a new token or get in contact with the Europeana APIs customer support via api@europeana.eu"),
  TOKEN_MISSING_401("401_token_missing","Token is missing","Please issue a token and supply it within the Authorization header."),
  TOKEN_EXPIRED_401("401_token_expired","Token has expired","Please acquire a new token by either log-in or refreshing using the refresh token"),
  KEY_INVALID_401("401_key_invalid", "API key is invalid", "Please register for an API key"),
  KEY_DISABLED_401("401_key_disabled", "You API key has been disabled", "Please register for a new API key"),
  USER_NOT_AUTHORIZED_403("403_user_not_authorised","User not authorised to access the resource","The user for which the token was granted for does not have sufficient rights to access the resource"),
  SCOPE_MISSING_403("403_scope_missing",
      "Client not authorised due to missing scope access",
      "The client does not have access to this service. Please get in contact with the Europeana APIs customer support via api@europeana.eu"),
  USER_MISSING_403("403_user_missing","",""),
  CLIENT_UNKNOWN_404("404_client_unknown","The client being requested is not known","The client id that was indicated in the request was not found in our records. Please confirm if the identifier corresponds to the public identifier for the client and its respective key.");

  private final String code;
  private final String error;
  private final String message;

  ErrorMessage(String code, String error, String message) {
    this.code = code;
    this.error = error;
    this.message = message;
  }
  public String getCode() {return code;}
  public String getError() {return error; }
  public String getMessage() {return message;}

}
