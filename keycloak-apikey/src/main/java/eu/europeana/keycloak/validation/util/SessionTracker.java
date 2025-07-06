package eu.europeana.keycloak.validation.util;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class SessionTracker implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes
    private String sessionId;
    private String userId;
    private LocalDateTime accessTime;
    private int sessionCount;
    public SessionTracker(String sessionId, String userId, int sessionCount) {
      this.sessionId = sessionId;
      this.userId = userId;
      this.accessTime = LocalDateTime.now();
      this.sessionCount = sessionCount;
    }

    // Getters and Setters (omitted for brevity, but you should include them)
    public String getSessionId() {
      return sessionId;
    }

    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public LocalDateTime getAccessTime() {
      return accessTime;
    }

    public void setAccessTime(LocalDateTime accessTime) {
      this.accessTime = accessTime;
    }

    public int getSessionCount() {
      return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
      this.sessionCount = sessionCount;
    }

    @Override
    public String toString() {
      return "SessionTracker{" +
          "sessionId='" + sessionId + '\'' +
          ", userId='" + userId + '\'' +
          ", accessTime=" + accessTime +
          ", sessionCount='" + sessionCount + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SessionTracker that = (SessionTracker) o;
      return Objects.equals(sessionId, that.sessionId) &&
          Objects.equals(userId, that.userId) &&
          Objects.equals(sessionCount, that.sessionCount);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sessionId, userId, sessionCount);
    }
  }