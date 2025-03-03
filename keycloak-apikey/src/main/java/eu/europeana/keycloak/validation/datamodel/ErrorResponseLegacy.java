package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import org.hibernate.jpa.internal.util.LogHelper;

@JsonPropertyOrder({"success", "status", "error", "message", "timestamp", "path"})
@JsonInclude(Include.NON_EMPTY)
public class ErrorResponseLegacy {
  @Getter
  private final boolean success = false;
  @Getter
  private final int status;
  @Getter
  private final String error;
  @Getter
  private final String message;
  @Getter
  private final String path;
  private final OffsetDateTime timestamp = OffsetDateTime.now();
  public ErrorResponseLegacy(int status, String error, String message,String path) {
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
  }
  public String getTimestamp() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    return fmt.format(timestamp);
  }
}