package eu.europeana.keycloak.registration.datamodel;

/**
 * Represents the structure of Error Response Object.
 */
public class ErrorResponse {
  private String error;
  private String message;
  private String code;

  /**
   * Construct new immutable instance
   * @param code error code  (e.g."400-account-unknown")
   * @param error High level error description
   * @param message Detailed errormessage string
   */
  public ErrorResponse(String code,String error, String message) {
    this.code = code;
    this.error = error;
    this.message =message;
  }

  public String getError() {
    return error;
  }

  public String getMessage() { return message; }

  public String getCode() { return code; }

}