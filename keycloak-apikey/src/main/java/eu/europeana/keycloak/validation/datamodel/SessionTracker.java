package eu.europeana.keycloak.validation.datamodel;

import eu.europeana.keycloak.validation.util.Constants;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SessionTracker implements Serializable {
  private static final long serialVersionUID = 1L;
  private String id;
  private int sessionCount;

  private LocalDateTime lastAccessDate;

  private String lastAccessDateString;

  public SessionTracker(String id, int sessionCount) {
    this.id = id;
    this.sessionCount = sessionCount;
    this.lastAccessDate = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATION_DATE_PATTERN).withZone(
        ZoneOffset.UTC);
    this.lastAccessDateString = formatter.format(lastAccessDate);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getSessionCount() {
    return sessionCount;
  }

  public void setSessionCount(int sessionCount) {
    this.sessionCount = sessionCount;
  }

  public LocalDateTime getLastAccessDate() {
    return lastAccessDate;
  }

  public void setLastAccessDate(LocalDateTime lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }

  public String getLastAccessDateString() {
    return lastAccessDateString;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    SessionTracker that = (SessionTracker) obj;
    return Objects.equals(id, that.id) &&
        Objects.equals(sessionCount, that.sessionCount) &&
        Objects.equals(lastAccessDate,that.lastAccessDate);
  }

}