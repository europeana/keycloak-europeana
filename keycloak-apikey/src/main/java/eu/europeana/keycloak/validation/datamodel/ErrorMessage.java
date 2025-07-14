package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Defines the Error messages returned by keycloak apikey module
 * The message structure has 3 parts viz.
 * code - string starting  with http code e.g. "401_key_invalid
 * error - Holds error  details e.g. "API key is invalid"
 * message - used mainly to define action to be taken by user for the error e.g. "Please register for an API key"
 */
@JsonPropertyOrder({"code", "error", "message"})
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorMessage {

  IP_MISSING_400("400_ip_missing","",""),
  IP_INVALID_400("400_ip_invalid","",""),
  DUPLICATE_KEY_400("400_duplicate_key","You already own a personal key","Users can only own one personal key which is intended for personal use."),
  KEY_LIMIT_REACHED_400("400_key_limit_reached",
      "You have reached the limit of personal keys",
      "A new personal key cannot be created because you reached a limit of disabled personal keys. "
          + "Please get in contact with the Europeana APIs customer support via api@europeana.eu"),
  TOKEN_INVALID_401("401_token_invalid",
      "Token is invalid",
      "Please acquire a new token or get in contact with the Europeana APIs customer support via api@europeana.eu"),
  TOKEN_MISSING_401("401_token_missing","Token is missing","Please issue a token and supply it within the Authorization header."),
  TOKEN_EXPIRED_401("401_token_expired","Token has expired","Please acquire a new token by either log-in or refreshing using the refresh token"),
  KEY_INVALID_401("401_key_invalid", "API key is invalid", "Please register for an API key"),
  KEY_DISABLED_401("401_key_disabled", "You API key has been disabled", "Please register for a new API key"),
  USER_NOT_AUTHORIZED_403("403_user_not_authorised",
      "User not authorised to access the resource",
      "The user for which the token was granted for does not have sufficient rights to access the resource"),
  SCOPE_MISSING_403("403_scope_missing",
      "Client not authorised due to missing scope access",
      "The client does not have access to this service. Please get in contact with the Europeana APIs customer support via api@europeana.eu"),
  USER_MISSING_403("403_user_missing","User information missing in Token",
      "The token was issued without authentication for the user. A ‘password’ or ‘authorization_code’ grant type is required to access this method."),
  CLIENT_UNKNOWN_404("404_client_unknown",
      "The client being requested is not known",
      "The client id that was indicated in the request was not found in our records. "
          + "Please confirm if the identifier corresponds to the public identifier for the client and its respective key."),
  CLIENT_ALREADY_DISABLED_410("410_client_disabled","The client has already disabled","The client has already been previously disabled."),

  LIMIT_PERSONAL_KEYS_429("429_limit_personal","Personal key client has reached the limit of %s requests per %s minutes","The use of personal keys is limited to X requests per Y minutes. If your project requires a higher rate limit or makes regular use of the APIs, we recommend applying for a project key in the account section of the Europeana website."),
  LIMIT_PROJECT_KEYS_429("429_limit_project","Project key client has reached the limit of %s request per %s minutes","The recommended way to access the Europeana APIs with project keys is by using access tokens, which can be obtained through the Europeana Authentication Service. Learn more at https://europeana.atlassian.net/wiki/spaces/EF/pages/2462351393/Accessing+the+APIs#Auth-Service");



  private final String code;
  @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
  private String error;
  @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
  private final String message;

  ErrorMessage(String code, String error, String message) {
    this.code = code;
    this.error = error;
    this.message = message;
  }
  public String getCode() {return code;}
  public String getError() {return error; }
  public String getMessage() {return message;}


  public ErrorMessage formatError(String ...args){
    this.error =  String.format(this.getError(),args);
    return this;
  }
}