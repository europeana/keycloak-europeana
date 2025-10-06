package eu.europeana.keycloak.zoho.datamodel;

import com.opencsv.bean.CsvBindByPosition;

public class APIProject {

  @CsvBindByPosition(position = 0)
  private String id;
  @CsvBindByPosition(position = 1)
  private String key;

  @CsvBindByPosition(position = 2)
  private String lastAccess;

  @CsvBindByPosition(position = 3)
  private String modifiedTime;

  public String getKey() {
    return key;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getLastAccess() {
    return lastAccess;
  }

  public void setLastAccess(String lastAccess) {
    this.lastAccess = lastAccess;
  }

  public String getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(String modifiedTime) {
    this.modifiedTime = modifiedTime;
  }
}