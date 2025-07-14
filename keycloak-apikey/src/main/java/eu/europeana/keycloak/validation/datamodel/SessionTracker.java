package eu.europeana.keycloak.validation.datamodel;

import java.io.Serializable;
import java.util.Objects;

public class SessionTracker implements Serializable {
  private static final long serialVersionUID = 1L;
  private String id;
  private String sessionCount;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSessionCount() {
    return sessionCount;
  }

  public void setSessionCount(String sessionCount) {
    this.sessionCount = sessionCount;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    SessionTracker that = (SessionTracker) obj;
    return Objects.equals(id, that.id) &&
        Objects.equals(sessionCount, that.sessionCount);
  }

}