package eu.europeana.keycloak.validation.datamodel;

import jakarta.ws.rs.core.Response.Status;
import org.keycloak.models.UserModel;

public class ValidationResult {
  private final Status httpStatus;
  private final ErrorMessage response;
  private  UserModel user;

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

  public UserModel getUser() {
    return user;
  }

  public void setUser(UserModel user) {
    this.user = user;
  }
}
