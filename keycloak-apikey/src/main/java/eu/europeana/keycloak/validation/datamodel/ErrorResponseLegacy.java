package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import lombok.Getter;


@JsonPropertyOrder({"success", "status", "error", "message", "timestamp", "path"})
@JsonInclude(Include.NON_EMPTY)
public class ErrorResponseLegacy {
  private final boolean success = false;
  private final int status;
  private final String error;
  private final String message;
  private final String path;
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private final OffsetDateTime timestamp = OffsetDateTime.now();
  public ErrorResponseLegacy(int status, String error, String message,String path) {
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
  }


}