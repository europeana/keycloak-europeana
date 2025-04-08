package eu.europeana.keycloak.validation.datamodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;
import lombok.Getter;
@JsonPropertyOrder({"id","type","client_id","name","description","state","creationDate"})
@JsonInclude(Include.NON_EMPTY)
public class Apikey {
  @Getter
  private final String id;
  @Getter
  private final String type;
  @Getter
  private final String client_id;
  @Getter
  private final String name;
  @Getter
  private final String description;
  @Getter
  private final String state;
  @Getter
  private final Date creationDate;

  public Apikey(String id ,String clientId, String type,Date creationDate,String name,String description,
      String state) {
    this.id = id;
    this.client_id = clientId;
    this.type = type;
    this.creationDate = creationDate;
    this.name =name;
    this.description =description;
    this.state = state;
  }
}
