package eu.europeana.keycloak.validation.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@JsonPropertyOrder({"success", "status", "error", "message", "timestamp", "path"})
@JsonInclude(Include.NON_EMPTY)
public class ErrorResponse {
  private final boolean success = false;
  private final int status;
  private final String error;
  private final String message;
  private final String path;
  @JsonFormat(
      pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  )
  private final OffsetDateTime timestamp = OffsetDateTime.now();
  public ErrorResponse(int status, String error, String message,String path) {
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
  }

  public boolean isSuccess() {return success; }

  public int getStatus() { return status; }

  public String getError() { return error; }

  public String getMessage() { return message; }

  public String getPath() { return path; }

  public String getTimestamp() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    return fmt.format(timestamp);
  }


}