package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;
import lombok.Getter;
@JsonPropertyOrder({"id","type","client_id","name","description","state","created"})
@JsonInclude(Include.NON_EMPTY)
public class Apikey {
  @Getter
  private final String id;
  @Getter
  private final String type;
  @JsonProperty("client_id")
  @Getter
  private final String clientId;
  @Getter
  private final String name;
  @Getter
  private final String description;
  @Getter
  private final String state;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
  @Getter
  private final Date created;

  public Apikey(String id ,String clientId, String type,Date creationDate,String name,String description,
      String state) {
    this.id = id;
    this.clientId = clientId;
    this.type = type;
    this.created = creationDate;
    this.name =name;
    this.description =description;
    this.state = state;
  }
}
