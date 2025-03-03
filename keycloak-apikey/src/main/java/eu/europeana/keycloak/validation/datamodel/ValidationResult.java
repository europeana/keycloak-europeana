package eu.europeana.keycloak.validation.datamodel;

import jakarta.ws.rs.core.Response.Status;

public class ValidationResult {
  private final Status httpStatus;
  private final ErrorMessage response;

  public ValidationResult(Status httpStatus, ErrorMessage response) {
    this.httpStatus = httpStatus;
    this.response = response;
  }
  public Status getHttpStatus() {
    return httpStatus;
  }
  public ErrorMessage getErrorResponse() {
    return response ;
  }

}
