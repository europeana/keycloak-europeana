package eu.europeana.keycloak.validation.datamodel;

public class ErrorResponse {
  private String error;
  private String message;
  private String code;

  public ErrorResponse(String code,String error, String message) {
    this.code = code;
    this.error = error;
    this.message =message;
  }
}
