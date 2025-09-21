package eu.europeana.keycloak.validation.datamodel;

import eu.europeana.keycloak.validation.util.Constants;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SessionTracker implements Serializable {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.CREATION_DATE_PATTERN).withZone(
      ZoneOffset.UTC);
  private static final long serialVersionUID = 1L;
  private String id;
  private int sessionCount;
  private LocalDateTime lastAccessDate;
  private LocalDateTime lastRateLimitReachingTime;

  public SessionTracker(String id, int sessionCount) {
    this.id = id;
    this.sessionCount = sessionCount;
    this.lastAccessDate = LocalDateTime.now();
   }

  public String getId() {
    return id;
  }

  public void setId(String id) {
   this.id=id;
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

  public LocalDateTime getLastRateLimitReachingTime() {
    return lastRateLimitReachingTime;
  }

  public void setLastRateLimitReachingTime(LocalDateTime lastRateLimitReachingTime) {
    this.lastRateLimitReachingTime = lastRateLimitReachingTime;
  }

  public String getLastAccessDateString() {
    return FORMATTER.format(lastAccessDate);
  }

  public String getLastRateLimitReachingTimeString() {
    if (lastRateLimitReachingTime != null) {
      return FORMATTER.format(lastRateLimitReachingTime);
    } else {
      return null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    SessionTracker that = (SessionTracker) obj;
    return Objects.equals(id, that.id) &&
        Objects.equals(sessionCount, that.sessionCount) &&
        Objects.equals(lastAccessDate,that.lastAccessDate)&&
        Objects.equals(lastRateLimitReachingTime,that.lastRateLimitReachingTime);
  }

}