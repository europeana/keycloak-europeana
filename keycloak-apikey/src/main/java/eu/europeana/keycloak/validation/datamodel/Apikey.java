package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;

@JsonInclude(Include.NON_EMPTY)
public class Apikey {
  @JsonIgnore
  @Getter
  private final String id;
  @Getter
  private final String client_id;
  @Getter
  private final String type;
  @Getter
  private final String  creationDate;
  @Getter
  private final String name;
  @Getter
  private final String description;

  public Apikey(String id ,String clientId, String type,String creationDate,String name,String description) {
    this.id = id;
    this.client_id = clientId;
    this.type = type;
    this.creationDate = creationDate;
    this.name =name;
    this.description =description;
  }
}
