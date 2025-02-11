package eu.europeana.keycloak.validation.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;

@JsonPropertyOrder({"success", "status", "error", "message", "timestamp", "path"})
@JsonInclude(Include.NON_EMPTY)
public class ErrorResponse {

  private final boolean success = false;
  private final int status;
  private final String error;
  private final String message;
  @JsonFormat(
      pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  )
  private final OffsetDateTime timestamp = OffsetDateTime.now();
  private final String trace;
  private final String path;
  private final String code;

  private ErrorResponse(int status, String error, String message, String trace, String path,
      String code) {
    this.status = status;
    this.error = error;
    this.message = message;
    this.trace = trace;
    this.path = path;
    this.code = code;
  }
}